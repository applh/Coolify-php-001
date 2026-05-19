<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { 
  Users, 
  UserPlus, 
  Cpu, 
  Activity, 
  PlusCircle, 
  Clock, 
  Trash2,
  Play,
  ClipboardList,
  ExternalLink
} from 'lucide-vue-next';
import BaseCard from '../components/BaseCard.vue';
import BaseButton from '../components/BaseButton.vue';
import BaseModal from '../components/BaseModal.vue';
import BaseInput from '../components/BaseInput.vue';

interface Agent {
  id: number;
  name: string;
  role: string;
  skills: string | string[];
  status: string;
  avatar_url?: string;
}

interface AgentTask {
  id: number;
  agent_id: number | null;
  agent_name?: string;
  title: string;
  description: string;
  status: 'queued' | 'in-progress' | 'completed' | 'failed';
  priority: number;
  input_data?: string;
  output_data?: string;
  created_at: string;
}

const agents = ref<Agent[]>([]);
const tasks = ref<AgentTask[]>([]);
const isLoading = ref(true);

const isAgentModalOpen = ref(false);
const isTaskModalOpen = ref(false);
const isOutputModalOpen = ref(false);
const selectedTask = ref<AgentTask | null>(null);

const newAgent = ref({
  name: '',
  role: '',
  skills_str: '',
  avatar_url: ''
});

const newTask = ref({
  agent_id: null as number | null,
  title: '',
  description: '',
  input_data: '',
  priority: 0
});

const fetchAgents = async () => {
  try {
    const res = await fetch('/api/agents');
    const data = await res.json();
    agents.value = data.map((a: Agent) => ({
      ...a,
      skills: typeof a.skills === 'string' ? JSON.parse(a.skills) : a.skills
    }));
  } catch (_err) {
    console.error('Failed to fetch agents:', _err);
  }
};

const fetchTasks = async () => {
  try {
    const res = await fetch('/api/agent-tasks');
    tasks.value = await res.json();
  } catch (_err) {
    console.error('Failed to fetch tasks:', _err);
  }
};

const createAgent = async () => {
  try {
    const skills = newAgent.value.skills_str.split(',').map(s => s.trim()).filter(Boolean);
    const res = await fetch('/api/agents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: newAgent.value.name,
        role: newAgent.value.role,
        skills,
        avatar_url: newAgent.value.avatar_url
      })
    });
    if (res.ok) {
      await fetchAgents();
      isAgentModalOpen.value = false;
      newAgent.value = { name: '', role: '', skills_str: '', avatar_url: '' };
    }
  } catch (_err) {
    console.error('Failed to create agent:', _err);
  }
};

const createTask = async () => {
  try {
    const res = await fetch('/api/agent-tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newTask.value)
    });
    if (res.ok) {
      await fetchTasks();
      isTaskModalOpen.value = false;
      newTask.value = { agent_id: null, title: '', description: '', input_data: '', priority: 0 };
    }
  } catch (_err) {
    console.error('Failed to create task:', _err);
  }
};

const deleteAgent = async (id: number) => {
  if (!confirm('Are you sure you want to delete this agent?')) return;
  try {
    await fetch(`/api/agents/${id}`, { method: 'DELETE' });
    await fetchAgents();
  } catch (_err) {
    console.error('Failed to delete agent:', _err);
  }
};

const runTask = async (task: AgentTask) => {
  if (task.status === 'in-progress') return;
  
  task.status = 'in-progress';
  try {
    const res = await fetch(`/api/agent-tasks/${task.id}/run`, {
      method: 'POST'
    });
    const data = await res.json();
    if (data.success) {
      await Promise.all([fetchAgents(), fetchTasks()]);
    } else {
      alert('Task failed: ' + data.error);
    }
  } catch (_err) {
    console.error('Failed to run task:', _err);
    task.status = 'failed';
  }
};

const viewTaskOutput = (task: AgentTask) => {
  selectedTask.value = task;
  isOutputModalOpen.value = true;
};

onMounted(async () => {
  await Promise.all([fetchAgents(), fetchTasks()]);
  isLoading.value = false;
});

const getStatusColor = (status: string) => {
  switch (status) {
    case 'completed': return 'text-green-500 bg-green-500/10';
    case 'in-progress': return 'text-blue-500 bg-blue-500/10';
    case 'failed': return 'text-red-500 bg-red-500/10';
    default: return 'text-white/40 bg-white/5';
  }
};
</script>

<template>
  <div class="px-6 py-8 flex flex-col gap-8 max-w-7xl mx-auto w-full">
    <!-- Header -->
    <div class="flex justify-between items-end">
      <div>
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 bg-white/5 rounded-lg flex items-center justify-center border border-white/10">
            <Users class="w-4 h-4 text-[#FF3B30]" />
          </div>
          <h1 class="text-3xl font-serif italic text-white tracking-tight">
            AI Agents Team
          </h1>
        </div>
        <p class="text-white/40 font-mono text-[10px] uppercase tracking-[0.2em]">
          Deployment Management / Autonomous Personnel
        </p>
      </div>
      <div class="flex gap-3">
        <BaseButton 
          variant="secondary"
          @click="isAgentModalOpen = true"
        >
          <UserPlus class="w-4 h-4 mr-2" />
          Recruit Agent
        </BaseButton>
        <BaseButton 
          @click="isTaskModalOpen = true"
        >
          <PlusCircle class="w-4 h-4 mr-2" />
          Assign Task
        </BaseButton>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Agents List -->
      <div class="lg:col-span-2 space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-serif italic text-white flex items-center gap-2">
            <Activity class="w-4 h-4 text-green-500" />
            Active Operatives
          </h2>
          <span class="text-[10px] font-mono text-white/20 uppercase tracking-widest">{{ agents.length }} Total</span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <BaseCard 
            v-for="agent in agents" 
            :key="agent.id"
            :title="agent.name"
            :subtitle="agent.role"
            hoverable
          >
            <template #icon>
              <Cpu class="w-24 h-24" />
            </template>
            
            <div class="mt-4 flex flex-wrap gap-2">
              <span 
                v-for="skill in agent.skills" 
                :key="skill"
                class="px-2 py-0.5 rounded-full bg-white/5 border border-white/5 text-[9px] font-mono text-white/50 uppercase tracking-tighter"
              >
                {{ skill }}
              </span>
            </div>

            <div class="mt-6 pt-4 border-t border-white/5 flex justify-between items-center">
              <div class="flex items-center gap-2 overflow-hidden">
                <div class="w-2 h-2 rounded-full bg-green-500" />
                <span class="text-[10px] font-mono text-white/40 uppercase tracking-widest truncate">{{ agent.status }}</span>
              </div>
              <button 
                class="text-white/20 hover:text-red-500 transition-colors p-1"
                @click="deleteAgent(agent.id)"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </BaseCard>
        </div>
      </div>

      <!-- Task Queue -->
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-serif italic text-white flex items-center gap-2">
            <ClipboardList class="w-4 h-4 text-[#FF3B30]" />
            Task Registry
          </h2>
          <span class="text-[10px] font-mono text-white/20 uppercase tracking-widest">{{ tasks.length }} Pending</span>
        </div>

        <div class="space-y-4">
          <div 
            v-for="task in tasks" 
            :key="task.id"
            class="bg-[#0F0F0F] border border-white/5 rounded-xl p-4 group hover:bg-[#141414] transition-all"
            :class="task.status === 'completed' ? 'cursor-pointer' : ''"
            @click="task.status === 'completed' ? viewTaskOutput(task) : null"
          >
            <div class="flex justify-between items-start mb-2">
              <h4 class="text-xs font-bold text-white uppercase tracking-wider">
                {{ task.title }}
              </h4>
              <span 
                class="px-2 py-0.5 rounded text-[8px] font-bold tracking-[0.1em] uppercase"
                :class="getStatusColor(task.status)"
              >
                {{ task.status }}
              </span>
            </div>
            <p class="text-[11px] text-white/40 line-clamp-2 mb-3 font-serif italic">
              {{ task.description }}
            </p>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <div class="w-5 h-5 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-[8px] font-bold text-white/40">
                  {{ task.agent_name?.[0] || '?' }}
                </div>
                <span class="text-[9px] font-mono text-white/30 uppercase tracking-tighter">Assigned: {{ task.agent_name || 'Unassigned' }}</span>
              </div>
              <div class="flex gap-2">
                <button 
                  v-if="task.status === 'completed'"
                  class="w-8 h-8 rounded-full flex items-center justify-center text-white/40 hover:text-green-500 hover:bg-white/5 transition-all"
                  title="View Output"
                  @click.stop="viewTaskOutput(task)"
                >
                  <ExternalLink class="w-3 h-3" />
                </button>
                <button 
                  v-if="task.status === 'queued'"
                  class="w-8 h-8 rounded-full flex items-center justify-center text-white/40 hover:text-[#FF3B30] hover:bg-white/5 transition-all"
                  title="Run Task"
                  @click.stop="runTask(task)"
                >
                  <Play class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>

          <div
            v-if="tasks.length === 0"
            class="text-center py-12 border border-dashed border-white/5 rounded-2xl"
          >
            <div class="flex justify-center mb-4">
              <Clock class="w-8 h-8 text-white/10" />
            </div>
            <p class="text-[10px] font-mono text-white/20 uppercase tracking-[0.2em]">
              Queue Empty
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->
    <BaseModal
      v-model="isAgentModalOpen"
      title="Recruit New AI Operative"
    >
      <div class="space-y-6">
        <BaseInput 
          v-model="newAgent.name"
          label="Agent Designation"
          placeholder="e.g. Aria, Kael, Echo"
        />
        <BaseInput 
          v-model="newAgent.role"
          label="Primary Function / Role"
          placeholder="e.g. System Architect, Security Specialist"
        />
        <BaseInput 
          v-model="newAgent.skills_str"
          label="Skills (comma separated)"
          placeholder="e.g. TypeScript, UI Design, Penetration Testing"
        />
        <BaseInput 
          v-model="newAgent.avatar_url"
          label="Avatar URL (optional)"
          placeholder="https://..."
        />
      </div>
      <template #footer>
        <BaseButton
          variant="secondary"
          @click="isAgentModalOpen = false"
        >
          Abort
        </BaseButton>
        <BaseButton @click="createAgent">
          Commit Recruitment
        </BaseButton>
      </template>
    </BaseModal>

    <BaseModal
      v-model="isTaskModalOpen"
      title="Register Assignment"
    >
      <div class="space-y-6">
        <BaseInput 
          v-model="newTask.title"
          label="Objective"
          placeholder="e.g. Audit API Endpoints"
        />
        <div class="flex flex-col gap-1.5">
          <label class="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30">Intelligence Briefing</label>
          <textarea 
            v-model="newTask.description"
            class="w-full bg-[#1A1A1A] border border-white/5 rounded-xl px-4 py-3 outline-none focus:border-[#FF3B30]/30 transition-all text-sm text-white placeholder:text-white/10 min-h-[100px]"
            placeholder="Detailed description of the task..."
          />
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30">Assign Operative</label>
          <select 
            v-model="newTask.agent_id"
            class="w-full bg-[#1A1A1A] border border-white/5 rounded-xl px-4 py-3 outline-none focus:border-[#FF3B30]/30 transition-all text-sm text-white"
          >
            <option :value="null">
              Unassigned (Queue for first available)
            </option>
            <option
              v-for="agent in agents"
              :key="agent.id"
              :value="agent.id"
            >
              {{ agent.name }} ({{ agent.role }})
            </option>
          </select>
        </div>
      </div>
      <template #footer>
        <BaseButton
          variant="secondary"
          @click="isTaskModalOpen = false"
        >
          Discard
        </BaseButton>
        <BaseButton @click="createTask">
          Deploy Task
        </BaseButton>
      </template>
    </BaseModal>

    <BaseModal
      v-model="isOutputModalOpen"
      :title="selectedTask?.title || 'Task Output'"
    >
      <div class="space-y-4">
        <div>
          <h4 class="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 mb-2">
            Intelligence Result
          </h4>
          <div class="bg-[#0A0A0A] border border-white/5 rounded-xl p-6 font-mono text-sm text-white/80 leading-relaxed whitespace-pre-wrap max-h-[60vh] overflow-y-auto">
            {{ selectedTask?.output_data || 'No output data available.' }}
          </div>
        </div>
      </div>
      <template #footer>
        <BaseButton @click="isOutputModalOpen = false">
          Dismiss
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

<?php

class AgentTeamManager {
    private static $teamsFile;

    private static function init($contentPath) {
        self::$teamsFile = $contentPath . '/agent_teams.json';
        if (!file_exists(self::$teamsFile)) {
            $defaultTeams = [
                'teams' => [
                    [
                        'id' => 'training-generator-team',
                        'name' => 'Training Content Team',
                        'agents' => [
                            [
                                'id' => 'slide-architect',
                                'role' => 'Curriculum Designer',
                                'skills' => ['slide-content-structuring', 'pedagogical-design'],
                                'commonSkills' => ['gemini-interaction']
                            ],
                            [
                                'id' => 'svg-illustrator',
                                'role' => 'Visual Designer',
                                'skills' => ['svg-generation', 'illustration-styling'],
                                'commonSkills' => ['gemini-interaction']
                            ]
                        ]
                    ]
                ]
            ];
            file_put_contents(self::$teamsFile, json_encode($defaultTeams, JSON_PRETTY_PRINT));
        }
    }

    public static function getTeams($contentPath) {
        self::init($contentPath);
        $data = json_decode(file_get_contents(self::$teamsFile), true);
        return $data['teams'] ?? [];
    }

    public static function getTeamById($contentPath, $teamId) {
        $teams = self::getTeams($contentPath);
        foreach ($teams as $team) {
            if ($team['id'] === $teamId) {
                return $team;
            }
        }
        return null;
    }

    public static function getAgentByRole($contentPath, $teamId, $role) {
        $team = self::getTeamById($contentPath, $teamId);
        if ($team) {
            foreach ($team['agents'] as $agent) {
                if ($agent['role'] === $role) {
                    return $agent;
                }
            }
        }
        return null;
    }
}

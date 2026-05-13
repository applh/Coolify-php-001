import React, { useState, useEffect } from 'react';
import sitesData from './sites.json';

type SiteData = {
  id: string;
  name: string;
  domain: string;
  description: string;
  theme: {
    primary: string;
    accent: string;
  };
  content: {
    hero: {
      title: string;
      subtitle: string;
      image: string;
    };
    features: Array<{ title: string; text: string }>;
  };
};

export default function App() {
  const [activeSite, setActiveSite] = useState<SiteData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Basic multi-domain detection logic
    const host = window.location.hostname;
    const urlParams = new URLSearchParams(window.location.search);
    const siteOverride = urlParams.get('__site');

    let site = sitesData.find(s => s.domain === host || s.id === siteOverride);
    
    // Default fallback
    if (!site) {
      site = sitesData[0];
    }
    
    setActiveSite(site);
    setLoading(false);
  }, []);

  if (loading) return <div className="flex items-center justify-center h-screen font-sans text-gray-500">Loading CMS...</div>;
  if (!activeSite) return <div className="flex items-center justify-center h-screen font-sans text-red-500">Site not found.</div>;

  return (
    <div className="min-h-screen font-sans bg-white" id="site-root">
      {/* Navigation */}
      <nav className="border-b border-gray-100 py-4 px-6 flex justify-between items-center" id="site-nav">
        <div className="text-xl font-bold" style={{ color: activeSite.theme.primary }}>{activeSite.name}</div>
        <div className="space-x-6 hidden md:flex text-gray-600">
          <a href="#" className="hover:text-gray-900 transition-colors">Accueil</a>
          <a href="#" className="hover:text-gray-900 transition-colors">Services</a>
          <a href="#" className="hover:text-gray-900 transition-colors">À Propos</a>
          <a href="#" className="hover:text-gray-900 transition-colors">Contact</a>
        </div>
        <button 
          className="px-4 py-2 rounded-lg text-white font-medium transition-all hover:brightness-110"
          style={{ backgroundColor: activeSite.theme.primary }}
          id="cta-nav"
        >
          S'inscrire
        </button>
      </nav>

      {/* Hero Section */}
      <header className="relative py-20 px-6 overflow-hidden" id="hero-section">
        <div className="max-w-6xl mx-auto grid md:grid-cols-2 gap-12 items-center relative z-10">
          <div className="space-y-6">
            <h1 className="text-5xl font-extrabold tracking-tight text-gray-900 leading-tight">
              {activeSite.content.hero.title}
            </h1>
            <p className="text-xl text-gray-600">
              {activeSite.content.hero.subtitle}
            </p>
            <div className="flex gap-4">
              <button 
                className="px-8 py-3 rounded-xl text-white font-bold text-lg shadow-lg"
                style={{ backgroundColor: activeSite.theme.primary }}
                id="hero-cta-main"
              >
                Découvrir
              </button>
              <button className="px-8 py-3 rounded-xl border-2 border-gray-200 text-gray-700 font-bold text-lg hover:bg-gray-50 bg-white" id="hero-cta-secondary">
                En savoir plus
              </button>
            </div>
          </div>
          <div className="relative group" id="hero-image-wrapper">
             <div className="absolute -inset-1 rounded-2xl blur opacity-25 group-hover:opacity-40 transition-opacity" style={{ background: activeSite.theme.primary }}></div>
             <img 
               src={activeSite.content.hero.image} 
               alt={activeSite.name} 
               className="relative rounded-2xl shadow-2xl w-full h-[400px] object-cover ring-1 ring-black/5"
               id="hero-image"
             />
          </div>
        </div>
        
        {/* Background blobs */}
        <div className="absolute top-0 right-0 -translate-y-1/2 translate-x-1/4 w-96 h-96 blur-3xl opacity-10 rounded-full" style={{ backgroundColor: activeSite.theme.primary }}></div>
        <div className="absolute bottom-0 left-0 translate-y-1/2 -translate-x-1/4 w-96 h-96 blur-3xl opacity-10 rounded-full" style={{ backgroundColor: activeSite.theme.accent }}></div>
      </header>

      {/* Features */}
      <section className="py-20 px-6 bg-gray-50/50" id="features-section">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-900">Nos Services</h2>
            <div className="h-1 w-20 mx-auto mt-4 rounded-full" style={{ backgroundColor: activeSite.theme.primary }}></div>
          </div>
          <div className="grid md:grid-cols-3 gap-8">
            {activeSite.content.features.map((feature, i) => (
              <div key={i} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow" id={`feature-${i}`}>
                <div 
                  className="w-12 h-12 rounded-xl mb-6 flex items-center justify-center text-white font-bold text-xl"
                  style={{ backgroundColor: activeSite.theme.primary }}
                >
                  {i + 1}
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">{feature.title}</h3>
                <p className="text-gray-600 leading-relaxed">{feature.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12 px-6" id="site-footer">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row justify-between items-center gap-8 text-center md:text-left">
          <div className="space-y-4">
            <div className="text-2xl font-bold text-white">{activeSite.name}</div>
            <p className="max-w-xs">{activeSite.description}</p>
          </div>
          <div className="flex gap-8">
            <a href="#" className="hover:text-white transition-colors">Mentions Légales</a>
            <a href="#" className="hover:text-white transition-colors">Confidentialité</a>
            <a href="#" className="hover:text-white transition-colors">Contact</a>
          </div>
          <div className="text-sm">
            © 2026 {activeSite.name}. Multi-stack CMS.
          </div>
        </div>
      </footer>
    </div>
  );
}

import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Gera .next/standalone: o Dockerfile copia só o que o servidor precisa, sem node_modules inteiro.
  output: "standalone",
  // Evita que o `next dev` crie um AGENTS.md (instruções para agentes de IA) dentro do repositório.
  agentRules: false,
};

export default nextConfig;

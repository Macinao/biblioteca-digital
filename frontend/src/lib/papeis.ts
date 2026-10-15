import type { Papel } from "@/types";

// Mesma hierarquia do backend: o bibliotecário também é professor, e o professor também é aluno.
const NIVEL: Record<Papel, number> = { ALUNO: 1, PROFESSOR: 2, BIBLIOTECARIO: 3 };

export function temPapel(papelDoUsuario: Papel | undefined, minimo: Papel): boolean {
  return papelDoUsuario !== undefined && NIVEL[papelDoUsuario] >= NIVEL[minimo];
}

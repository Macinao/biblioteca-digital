import { temPapel } from "../papeis";

describe("temPapel (hierarquia igual à do backend)", () => {
  it.each([
    ["BIBLIOTECARIO", "BIBLIOTECARIO", true],
    ["BIBLIOTECARIO", "PROFESSOR", true],
    ["BIBLIOTECARIO", "ALUNO", true],
    ["PROFESSOR", "BIBLIOTECARIO", false],
    ["PROFESSOR", "PROFESSOR", true],
    ["PROFESSOR", "ALUNO", true],
    ["ALUNO", "PROFESSOR", false],
    ["ALUNO", "BIBLIOTECARIO", false],
    ["ALUNO", "ALUNO", true],
  ] as const)("%s com mínimo %s -> %s", (papel, minimo, esperado) => {
    expect(temPapel(papel, minimo)).toBe(esperado);
  });

  it("sem usuário nunca tem papel", () => {
    expect(temPapel(undefined, "ALUNO")).toBe(false);
  });
});

import { lerTotal, totalDePaginas } from "../paginacao";

describe("paginação", () => {
  it("lê o total do header X-Total-Count (nome em minúsculas, como o axios entrega)", () => {
    expect(lerTotal({ "x-total-count": "11013" }, [1, 2, 3])).toBe(11013);
  });

  it("sem o header (ou com lixo) o total é o que veio", () => {
    expect(lerTotal({}, [1, 2, 3])).toBe(3);
    expect(lerTotal({ "x-total-count": "abc" }, [1, 2])).toBe(2);
  });

  it.each([
    [0, 20, 1],
    [1, 20, 1],
    [20, 20, 1],
    [21, 20, 2],
    [10050, 24, 419],
  ])("%i itens em páginas de %i = %i página(s)", (total, tamanho, esperado) => {
    expect(totalDePaginas(total, tamanho)).toBe(esperado);
  });
});

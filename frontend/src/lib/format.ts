/** "2020-05-01T00:00:00" -> "01/05/2020". Sem fuso: o backend manda LocalDateTime. */
export function formatarData(iso?: string | null): string {
  if (!iso) return "—";
  const data = new Date(iso);
  return Number.isNaN(data.getTime()) ? "—" : data.toLocaleDateString("pt-BR");
}

/** Texto do <input type="date"> ("2020-05-01") para o LocalDateTime que a API espera. */
export function dataParaApi(dataDoInput: string): string {
  return dataDoInput.includes("T") ? dataDoInput : `${dataDoInput}T00:00:00`;
}

/** Caminho inverso: LocalDateTime da API -> valor do <input type="date">. */
export function dataParaInput(iso?: string | null): string {
  return iso ? iso.slice(0, 10) : "";
}

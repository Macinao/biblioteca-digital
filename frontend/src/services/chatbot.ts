import { api } from "@/lib/api";

export const chatbotService = {
  /** A memória da conversa fica no servidor (Redis), por usuário: o front só envia a mensagem nova. */
  async enviar(mensagem: string): Promise<string> {
    const { data } = await api.post<string>("/chatbot/mensagem", { mensagem });
    return data;
  },
};

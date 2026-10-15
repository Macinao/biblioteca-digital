"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import PaginaPrivada from "@/components/PaginaPrivada";
import { useCadastrarUsuario } from "@/hooks/useGerenciar";
import { mensagemDeErro } from "@/lib/api";
import { ROTULO_PAPEL, type NovoUsuario, type Papel } from "@/types";

const PAPEIS = Object.keys(ROTULO_PAPEL) as Papel[];
const classeCampo = "w-full rounded border border-gray-300 px-3 py-2";

// Mesmas regras do backend (CadastrarUsuarioRequest): o erro aparece antes de ir ao servidor.
const REGRAS = {
  nome: { required: "Informe o nome.", pattern: { value: /^[A-Za-zÀ-ÿ' -]+$/, message: "Use só letras, espaços, apóstrofo e hífen." }, minLength: { value: 2, message: "Mínimo de 2 caracteres." } },
  matricula: { required: "Informe a matrícula.", pattern: { value: /^[a-zA-Z0-9]{6,20}$/, message: "Use de 6 a 20 letras ou números." } },
  senha: { required: "Informe a senha.", pattern: { value: /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,64}$/, message: "De 8 a 64 caracteres, com maiúscula, minúscula e número." } },
  email: { required: "Informe o e-mail.", pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: "E-mail inválido." } },
  telefone: { required: "Informe o telefone.", pattern: { value: /^\(?\d{2}\)?\s?9?\d{4}-?\d{4}$/, message: "Telefone inválido. Ex.: (83) 99999-0000." } },
};

export default function Usuarios() {
  return (
    <PaginaPrivada papelMinimo="BIBLIOTECARIO">
      <Conteudo />
    </PaginaPrivada>
  );
}

function Conteudo() {
  const cadastrar = useCadastrarUsuario();
  const [aviso, setAviso] = useState<{ tipo: "ok" | "erro"; texto: string } | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NovoUsuario>({ defaultValues: { role: "ALUNO" } });

  function aoEnviar(dados: NovoUsuario) {
    setAviso(null);
    cadastrar.mutate(dados, {
      onSuccess: () => {
        setAviso({ tipo: "ok", texto: `Usuário ${dados.matricula} cadastrado.` });
        reset({ role: "ALUNO" });
      },
      onError: (e) => setAviso({ tipo: "erro", texto: mensagemDeErro(e, "Não foi possível cadastrar.") }),
    });
  }

  const campo = (id: keyof NovoUsuario, rotulo: string, tipo = "text", autoComplete = "off") => (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-gray-700">{rotulo}</label>
      <input id={id} type={tipo} autoComplete={autoComplete} aria-invalid={Boolean(errors[id])} className={classeCampo} {...register(id, REGRAS[id as keyof typeof REGRAS])} />
      {errors[id] && <p className="mt-1 text-sm text-red-700">{errors[id]?.message}</p>}
    </div>
  );

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">Cadastrar usuário</h1>
      <p className="mt-1 text-sm text-gray-600">Só o bibliotecário cria contas. A pessoa entra com a matrícula e a senha definidas aqui.</p>

      <form onSubmit={handleSubmit(aoEnviar)} noValidate className="mt-4 max-w-xl space-y-4 rounded-lg bg-white p-5 shadow-sm">
        {campo("nome", "Nome completo")}
        {campo("matricula", "Matrícula")}
        {campo("senha", "Senha inicial", "password", "new-password")}
        {campo("email", "E-mail", "email")}
        {campo("telefone", "Telefone", "tel")}
        <div>
          <label htmlFor="role" className="mb-1 block text-sm font-medium text-gray-700">Papel</label>
          <select id="role" className={classeCampo} {...register("role")}>
            {PAPEIS.map((p) => (
              <option key={p} value={p}>{ROTULO_PAPEL[p]}</option>
            ))}
          </select>
        </div>

        {aviso && (
          <p role={aviso.tipo === "erro" ? "alert" : "status"} className={`rounded p-2 text-sm ${aviso.tipo === "erro" ? "bg-red-50 text-red-800" : "bg-green-50 text-green-800"}`}>
            {aviso.texto}
          </p>
        )}

        <button type="submit" disabled={cadastrar.isPending} className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900 disabled:opacity-60">
          {cadastrar.isPending ? "Cadastrando…" : "Cadastrar"}
        </button>
      </form>
    </>
  );
}

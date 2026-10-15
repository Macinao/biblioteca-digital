package com.digital.smartteca.common.commands;

import com.digital.smartteca.common.enums.BucketSupabase;

public record DeletarArquivoCommand(
        BucketSupabase bucket,
        String nome
) {
}

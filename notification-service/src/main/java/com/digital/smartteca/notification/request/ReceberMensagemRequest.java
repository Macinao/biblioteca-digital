package com.digital.smartteca.notification.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ReceberMensagemRequest {

    String number;
    String text;

}

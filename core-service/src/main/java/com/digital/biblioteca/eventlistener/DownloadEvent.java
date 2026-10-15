package com.digital.biblioteca.eventlistener;

import com.digital.smartteca.common.enums.BucketSupabase;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DownloadEvent {

    String link;

    BucketSupabase bucket;

}

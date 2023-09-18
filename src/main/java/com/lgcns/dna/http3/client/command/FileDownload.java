package com.lgcns.dna.http3.client.command;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lgcns.dna.http3.client.util.HttpUtil;

@ShellComponent
public class FileDownload {
  @ShellMethod(key = "download")
  public String download() throws Exception {
    HttpUtil.downloadChunk();

    return "Download complete using HTTP/2";
  }

  @ShellMethod(key = "download-async")
  public String downloadAsync() throws Exception {
    HttpUtil.downloadChunkAsync();

    return "Download complete using Async HTTP/2";
  }

  @ShellMethod(key = "download3")
  public String download3() throws Exception {
    HttpUtil.downloadChunkByHttp3();

    return "Download complete using HTTP/3";
  }

  @ShellMethod(key = "download3-async")
  public String download3Async() throws Exception {
    HttpUtil.downloadChunkByAsyncHttp3();

    return "Download complete using Async HTTP/3";
  }
}

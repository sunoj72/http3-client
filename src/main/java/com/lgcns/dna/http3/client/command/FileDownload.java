package com.lgcns.dna.http3.client.command;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lgcns.dna.http3.client.util.HttpUtil;

@ShellComponent
public class FileDownload {
  @ShellMethod(key = "download")
  public String upload() throws Exception {
    HttpUtil.downloadChunk();
    return "done";
  }

  @ShellMethod(key = "download3")
  public String upload3() throws Exception {
    HttpUtil.downloadChunkByHttp3();
    return "done";
  }
}

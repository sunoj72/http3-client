package com.lgcns.dna.http3.client.command;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lgcns.dna.http3.client.util.HttpUtil;

@ShellComponent
public class FileUpload {
  @ShellMethod(key = "upload")
  public String upload() throws Exception {
    HttpUtil.uploadChunk();

    return "Upload complete using HTTP/2";
  }

  @ShellMethod(key = "upload3")
  public String upload3() throws Exception {
    HttpUtil.uploadChunk();

    return "Upload complete using HTTP/3";
  }
}

package com.kalsym.facebook.wrapper.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** @author Sarosh */
@Getter
@Setter
public class MenuItem {

  @NotNull
  @Size(min = 2, max = 10)
  private String type;

  @NotNull
  @Size(min = 2, max = 20)
  private String title;

  @NotNull
  @Size(min = 1, max = 1000)
  private String payload;
}

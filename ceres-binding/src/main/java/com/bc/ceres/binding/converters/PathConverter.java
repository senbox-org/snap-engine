package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;

import java.nio.file.Path;

public class PathConverter implements Converter<Path> {

  @Override
  public Class<Path> getValueType() {
    return Path.class;
  }

  @Override
  public Path parse(String text) {
    if (text.isEmpty()) {
      return null;
    }
    return Path.of(text);
  }

  @Override
  public String format(Path value) {
    if (value == null) {
      return "";
    }
    return value.toAbsolutePath().toString();
  }
}

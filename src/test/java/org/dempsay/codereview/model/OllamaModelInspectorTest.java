package org.dempsay.codereview.model;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class OllamaModelInspectorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void parseContextTokensReadsNumCtxFromParameters() throws Exception {
    final String json = """
        {
          "parameters": "temperature 0.2\\nnum_ctx 262144",
          "model_info": {}
        }
        """;

    assertEquals(262144, OllamaModelInspector.parseContextTokens(MAPPER.readTree(json)));
  }

  @Test
  public void parseContextTokensFallsBackToModelInfoContextLength() throws Exception {
    final String json = """
        {
          "parameters": "temperature 0.2",
          "model_info": {
            "qwen3.context_length": 131072
          }
        }
        """;

    assertEquals(131072, OllamaModelInspector.parseContextTokens(MAPPER.readTree(json)));
  }

  @Test
  public void parseContextTokensPrefersNumCtxOverModelInfo() throws Exception {
    final String json = """
        {
          "parameters": "num_ctx 8192",
          "model_info": {
            "qwen3.context_length": 131072
          }
        }
        """;

    assertEquals(8192, OllamaModelInspector.parseContextTokens(MAPPER.readTree(json)));
  }
}
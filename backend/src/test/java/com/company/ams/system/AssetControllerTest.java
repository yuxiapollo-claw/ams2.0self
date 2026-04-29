package com.company.ams.system;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import com.company.ams.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssetController.class)
@Import(SecurityConfig.class)
class AssetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService assetService;

    @Test
    @WithMockUser
    void treeReturnsTypedAssetNodes() throws Exception {
        given(assetService.tree()).willReturn(List.of(
                new AssetNode(
                        1L,
                        "仪器设备",
                        "CATEGORY",
                        List.of(new AssetNode(
                                100L,
                                "设备A",
                                "DEVICE",
                                List.of(new AssetNode(
                                        300L,
                                        "操作员",
                                        "ROLE",
                                        List.of()
                                ))
                        ))
                )
        ));

        mockMvc.perform(get("/api/assets/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].nodeName").value("仪器设备"))
                .andExpect(jsonPath("$.data[0].children[0].id").value(100))
                .andExpect(jsonPath("$.data[0].children[0].nodeType").value("DEVICE"))
                .andExpect(jsonPath("$.data[0].children[0].children[0].nodeName").value("操作员"))
                .andExpect(jsonPath("$.data[0].children[0].children[0].nodeType").value("ROLE"));
    }
}

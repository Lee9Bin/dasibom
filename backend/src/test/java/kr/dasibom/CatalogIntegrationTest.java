package kr.dasibom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import jakarta.servlet.http.Cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class CatalogIntegrationTest {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void configure(DynamicPropertyRegistry p){p.add("spring.datasource.url",mysql::getJdbcUrl);p.add("spring.datasource.username",mysql::getUsername);p.add("spring.datasource.password",mysql::getPassword);}
    @Autowired MockMvc mvc;
    @Test void migratesAndFiltersRealRegions()throws Exception{
        mvc.perform(get("/api/v1/regions").param("q","원주")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].code").value("51130")).andExpect(jsonPath("$.items[0].hiddenScore").isEmpty());
        mvc.perform(get("/api/v1/regions").param("size","301")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/regions/99999")).andExpect(status().isNotFound());
    }
    @Test void likesAreIdempotentAndRejectCrossOrigin()throws Exception{
        var cookie=new Cookie("dasibom_visitor","35a19bea-85bd-4e93-96c7-54b8c220bb31");
        for(int i=0;i<2;i++)mvc.perform(put("/api/v1/regions/51130/like").header("Origin","http://localhost:3000").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(put("/api/v1/regions/51130/like").header("Origin","https://evil.example")).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(delete("/api/v1/regions/51130/like").header("Origin","http://localhost:3000").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }
    @Test void removedSyncApiIsNotPublic()throws Exception{mvc.perform(post("/api/v1/admin/sync/catalog")).andExpect(status().isNotFound());}
}

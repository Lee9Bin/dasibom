package kr.dasibom.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.dasibom.domain.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class DatasetStore {
    private final DatasetRepository repository;
    private final ObjectMapper mapper;
    public DatasetStore(DatasetRepository repository, ObjectMapper mapper) { this.repository=repository; this.mapper=mapper; }
    public void save(String code, String kind, List<JsonNode> data) {
        try {
            repository.save(new Dataset(code,kind,mapper.writeValueAsString(data)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Dataset serialization failed"); }
    }
    public List<JsonNode> read(String code, String kind) {
        var row = repository.findById(new Dataset.Id(code,kind));
        if (row.isEmpty()) return List.of();
        try { return mapper.readValue(row.get().getPayload(),new TypeReference<List<JsonNode>>(){}); }
        catch (Exception e) { throw new IllegalStateException("Stored dataset is invalid"); }
    }
    public List<Map<String,Object>> status() {
        return repository.findAll().stream().map(d->Map.<String,Object>of("regionCode",d.getId().getRegionCode(),"kind",d.getId().getKind(),"fetchedAt",d.getFetchedAt())).toList();
    }
}

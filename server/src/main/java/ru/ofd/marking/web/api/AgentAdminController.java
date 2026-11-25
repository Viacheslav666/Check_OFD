package ru.ofd.marking.web.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ofd.marking.agents.AgentAdminService;
import ru.ofd.marking.agents.AgentSyncService;
import ru.ofd.marking.web.dto.AgentRequest;
import ru.ofd.marking.web.dto.AgentView;

/** ЛК приложения: реестр агентов и их статусы. Защищено тем же X-Api-Key, что и остальной /api. */
@RestController
@RequestMapping("/api")
public class AgentAdminController {

    private final AgentAdminService admin;
    private final AgentSyncService sync;

    public AgentAdminController(AgentAdminService admin, AgentSyncService sync) {
        this.admin = admin;
        this.sync = sync;
    }

    @GetMapping("/agents")
    public List<AgentView> agents() {
        return admin.list();
    }

    @GetMapping("/agents/{id}")
    public AgentView agent(@PathVariable Long id) {
        return admin.get(id);
    }

    @PostMapping("/admin/agents")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentView create(@RequestBody AgentRequest r) {
        return admin.create(r);
    }

    @PutMapping("/admin/agents/{id}")
    public AgentView update(@PathVariable Long id, @RequestBody AgentRequest r) {
        return admin.update(id, r);
    }

    @DeleteMapping("/admin/agents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        admin.delete(id);
    }

    @PostMapping("/admin/agents/{id}/test")
    public Map<String, Object> test(@PathVariable Long id) {
        return admin.test(id);
    }

    @PostMapping("/admin/agents/{id}/poll")
    public AgentSyncService.PollResult poll(@PathVariable Long id) {
        admin.get(id);
        return sync.pollAgent(id);
    }

    @PostMapping("/admin/agents/{id}/reset-cursor")
    public AgentView resetCursor(@PathVariable Long id) {
        admin.get(id);
        sync.resetCursor(id);
        return admin.get(id);
    }
}

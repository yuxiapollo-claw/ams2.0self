package com.company.ams.admin;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/mail-templates")
public class MailTemplateController {
    private final MailTemplateService mailTemplateService;

    public MailTemplateController(MailTemplateService mailTemplateService) {
        this.mailTemplateService = mailTemplateService;
    }

    @GetMapping
    public ApiResponse<ListPayload<MailTemplateRow>> list() {
        List<MailTemplateRow> rows = mailTemplateService.list();
        return ApiResponse.success(new ListPayload<>(rows, rows.size()));
    }

    @PostMapping
    public ApiResponse<MailTemplateRow> create(@RequestBody MailTemplateUpsertCommand command) {
        return ApiResponse.success(mailTemplateService.create(command));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<MailTemplateRow> update(
            @PathVariable long templateId,
            @RequestBody MailTemplateUpsertCommand command) {
        return ApiResponse.success(mailTemplateService.update(templateId, command));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable long templateId) {
        mailTemplateService.delete(templateId);
        return ApiResponse.success(null);
    }
}

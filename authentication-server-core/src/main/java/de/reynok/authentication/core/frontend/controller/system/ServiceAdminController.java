package de.reynok.authentication.core.frontend.controller.system;

import de.reynok.authentication.core.backend.configuration.WebRequiresAuthentication;
import io.virtuellewolke.authentication.core.database.entity.Service;
import io.virtuellewolke.authentication.core.spring.components.ServiceValidation;
import io.virtuellewolke.authentication.core.database.repository.IdentityRepository;
import io.virtuellewolke.authentication.core.database.repository.ServiceRepository;
import de.reynok.authentication.core.frontend.controller.AbstractAuthyController;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

@RestController
public class ServiceAdminController extends AbstractAuthyController {

    private ServiceRepository serviceRepository;
    private ServiceValidation serviceValidation;

    public ServiceAdminController(ServiceValidation serviceValidation, IdentityRepository identityRepository, ServiceRepository serviceRepository) {
        super(identityRepository);
        this.serviceRepository = serviceRepository;
        this.serviceValidation = serviceValidation;
    }

    @WebRequiresAuthentication(adminOnly = true)
    @GetMapping("/api/admin/routes")
    public List<Service> getServices() {
        return serviceRepository.findAll();
    }

    @WebRequiresAuthentication(adminOnly = true)
    @GetMapping("/api/admin/route/{id}")
    public Service getService(@PathVariable("id") int id) {
        return serviceRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @WebRequiresAuthentication(adminOnly = true)
    @DeleteMapping("/api/admin/route/{id}")
    public void deleteService(@PathVariable("id") int id) {
        serviceRepository.deleteById(id);
    }

    @WebRequiresAuthentication(adminOnly = true)
    @PostMapping("/api/admin/routes")
    public Service createService(@RequestBody Service service) {
        service.setId(null);
        serviceRepository.save(service);
        return service;
    }

    @WebRequiresAuthentication(adminOnly = true)
    @PatchMapping("/api/admin/route/{id}")
    public Service patchService(@RequestBody Map<String, Object> data, @PathVariable("id") int id) {
        Service service = serviceRepository.findById(id).orElseThrow(EntityNotFoundException::new);

        service.updateFrom(data);

        serviceRepository.save(service);

        return service;
    }

    @WebRequiresAuthentication(adminOnly = true)
    @GetMapping("/api/admin/route/find")
    public Service getServiceByUrl(@RequestParam("service") String url) {
        Service service = serviceValidation.getRegisteredServiceFor(url);
        if (service != null) return service;
        throw new EntityNotFoundException("Service for " + url + " not found.");
    }
}
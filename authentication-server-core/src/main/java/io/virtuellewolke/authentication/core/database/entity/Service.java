package io.virtuellewolke.authentication.core.database.entity;

import io.virtuellewolke.authentication.core.util.ListConverter;
import io.virtuellewolke.authentication.core.util.PartialUpdateableModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Slf4j
@ToString(exclude = {"allowedUrls", "requiredRoles"})
public class Service extends PartialUpdateableModel {

    public enum ServiceMode {
        ANONYMOUS, PUBLIC, ADMIN, AUTHORIZED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer      id;
    private String       name;
    private Boolean      enabled       = true;
    @Convert(converter = ListConverter.class)
    private List<String> allowedUrls   = new ArrayList<>();
    @Convert(converter = ListConverter.class)
    private List<String> requiredRoles = new ArrayList<>();
    @Enumerated(value = EnumType.STRING)
    private ServiceMode  mode          = ServiceMode.AUTHORIZED;

    public boolean isIdentityAllowed(Identity identity) {
        boolean isAllowed = false;

        switch (mode) {
            case ANONYMOUS:
                isAllowed = true;
                break;
            case ADMIN:
                isAllowed = identity != null && identity.getAdmin() && !identity.getLocked();
                break;
            case PUBLIC:
                isAllowed = identity != null && !identity.getLocked();
                break;
            case AUTHORIZED:
                isAllowed = identity != null && !identity.getLocked() && this.getRequiredRoles().stream().anyMatch(roleName -> identity.getAuthorities().stream().anyMatch(authority -> authority.getName().equals(roleName)));
                break;
        }

        return isAllowed;
    }

    public boolean isIdentityNotAllowed(Identity identity) {
        return !isIdentityAllowed(identity);
    }

    @Override
    protected void onUpdateField(Field field, Object value) {
        if (field.getName().equals("mode")) {
            setMode(ServiceMode.valueOf(value.toString()));
        } else {
            super.onUpdateField(field, value);
        }
    }
}
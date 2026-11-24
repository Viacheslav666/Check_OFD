package ru.ofd.marking.domain.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.AppSetting;
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {}

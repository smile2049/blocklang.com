package com.blocklang.release.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blocklang.release.model.Installer;

public interface InstallerDao extends JpaRepository<Installer, Integer>{

}
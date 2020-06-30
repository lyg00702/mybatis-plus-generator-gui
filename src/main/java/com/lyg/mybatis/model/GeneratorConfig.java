package com.lyg.mybatis.model;

import lombok.Data;

/**
 *
 * GeneratorConfig is the Config of mybatis generator config exclude database
 * config
 *
 * Created by Owen on 6/16/16.
 */
@Data
public class GeneratorConfig {

	/**
	 * 本配置的名称
	 */
	private String name;

	private String projectFolder;

	private String projectPackage;

	private String modelPackageTargetFolder;

	private String moduleName;

	private String domainObjectName;

	private String author;

	private String superController;

	private String logicDeletedField;

	private String encoding;

	private boolean overrideEntity;

	private boolean supportLombok;

	private boolean genController;

}

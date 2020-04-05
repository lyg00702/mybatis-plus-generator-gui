package com.lyg.mybatis.controller;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.lyg.mybatis.config.FXMLPage;
import com.lyg.mybatis.model.DatabaseConfig;
import com.lyg.mybatis.model.DbType;
import com.lyg.mybatis.model.GeneratorConfig;
import com.lyg.mybatis.util.ConfigHelper;
import com.lyg.mybatis.util.DbUtil;
import com.lyg.mybatis.view.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author Carl Lee
 * @date 2020-04-04 9:01
 */
public class MainUIController  extends BaseFXController{

    private static final Logger _LOG = LoggerFactory.getLogger(MainUIController.class);
    private static final String FOLDER_NO_EXIST = "部分目录不存在，是否创建";
    @FXML
    private Label lblConnection;
    @FXML
    private Label lblConfigs;
    @FXML
    private TextField txtTargetProject;
    @FXML
    private TextField txtModuleName;
    @FXML
    private TextField txtSuperController;
    @FXML
    private TextField txtProjectPackage;
    @FXML
    private TextField txtProjectFolder;
    @FXML
    private TextField txtAuthor;
    
    @FXML
    private CheckBox cbSupportLombok;
    @FXML
    private CheckBox cbOverrideEntity;
    @FXML
    private CheckBox cbGenController;
    @FXML
    private ListView<String> listViewTables;

    @FXML
    private TreeView<String> tvLeftDB;
    @FXML
    private ChoiceBox<String> choiceBoxEncoding;

    /**
     * Current selected databaseConfig
     */
    private DatabaseConfig selectedDatabaseConfig;

    /**
     * Current selected tableName
     */
    private String tableName;

    private final static String  SELECTED_BACKGROUND = "-fx-background-color: #82D900";

    /**
     * 主页面 UI 初始化
     * @param location
     * @param resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ImageView dbImage = new ImageView("icons/computer.png");
        dbImage.setFitHeight(40);
        dbImage.setFitWidth(40);
        lblConnection.setGraphic(dbImage);
        lblConnection.setOnMouseClicked(event -> {
            DbConnectionController controller = (DbConnectionController) loadFXMLPage("新建数据库连接", FXMLPage.NEW_CONNECTION, false);
            controller.setMainUIController(this);
            controller.showDialogStage();
        });

        ImageView configImage = new ImageView("icons/config-list.png");
        configImage.setFitHeight(40);
        configImage.setFitWidth(40);
        lblConfigs.setGraphic(configImage);
        lblConfigs.setOnMouseClicked(event -> {
            GeneratorConfigController controller = (GeneratorConfigController) loadFXMLPage("配置", FXMLPage.GENERATOR_CONFIG, false);
            controller.setMainUIController(this);
            controller.showDialogStage();
        });

        this.leftDBInitial();

        setTooltip();
        //默认选中第一个，否则如果忘记选择，没有对应错误提示
        choiceBoxEncoding.getSelectionModel().selectFirst();
    }


    /**
     * 选择项目 文件夹
     */
    @FXML
    public void chooseProjectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedFolder = directoryChooser.showDialog(getPrimaryStage());
        if (selectedFolder != null) {
            txtProjectFolder.setText(selectedFolder.getAbsolutePath());
        }
    }


    /**
     * 生成代码
     */
    @FXML
    public void generateCode() throws ClassNotFoundException {
        if (listViewTables == null || listViewTables.getItems().isEmpty()) {
            AlertUtil.showWarnAlert("请先在左侧选择数据库表");
            return;
        }
        String result = validateConfig();
        if (result != null) {
            AlertUtil.showErrorAlert(result);
            return;
        }
        GeneratorConfig generatorConfig = getGeneratorConfigFromUI();
        if (!checkDirs(generatorConfig)) {
            return;
        }

        this.mybatisPlusGenerator();

        AlertUtil.showInfoAlert("代码已生成!");
    }

    private void mybatisPlusGenerator() throws ClassNotFoundException {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = txtProjectFolder.getText();
        gc.setOutputDir(projectPath + File.separator +  txtTargetProject.getText());
        gc.setAuthor(txtAuthor.getText());
        gc.setMapperName("%sDao");
//        gc.setFileOverride(true);
        gc.setOpen(false);
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DbType dbType = DbType.valueOf(selectedDatabaseConfig.getDbType());
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl(DbUtil.getConnectionUrlWithSchema(selectedDatabaseConfig));
        // dsc.setSchemaName("public");
        dsc.setDriverName(dbType.getDriverClass());
        dsc.setUsername(selectedDatabaseConfig.getUsername());
        dsc.setPassword(selectedDatabaseConfig.getPassword());
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        // 设置模块名
        pc.setModuleName(txtModuleName.getText().trim());
        pc.setParent(txtProjectPackage.getText());
        pc.setMapper("dao");
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // to do nothing
            }
        };
        List<FileOutConfig> focList = new ArrayList<>();
        focList.add(new FileOutConfig("/templates/mapper.xml.ftl") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                // 自定义输入文件名称
                return projectPath + "/src/main/resources/mapper/" + pc.getModuleName()
                        + "/" + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
            }
        });
        cfg.setFileOutConfigList(focList);
        mpg.setCfg(cfg);
        mpg.setTemplate(new TemplateConfig().setXml(null));

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
//        strategy.setSuperEntityClass("com.baomidou.mybatisplus.samples.generator.common.BaseEntity");
        strategy.setEntityLombokModel(cbSupportLombok.isSelected());

        if(StringUtils.isNotBlank(txtSuperController.getText())){
            //设置BaseController
            strategy.setSuperControllerClass(txtSuperController.getText());
        }

        //把ListView 的数据转成 数组
        List<String> lstTable = new ArrayList<>(16);
        for(String table : listViewTables.getItems()){
            lstTable.add(table);
        }
        String[] arrTable = lstTable.toArray(new String[lstTable.size()]);

        strategy.setEntityColumnConstant(true);
        strategy.setInclude(arrTable);
        strategy.setSuperEntityColumns("id");
        strategy.setControllerMappingHyphenStyle(true);
        strategy.setTablePrefix(pc.getModuleName() + "_");
        mpg.setStrategy(strategy);
        // 选择 freemarker 引擎需要指定如下加，注意 pom 依赖必须有！
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }

    /**
     * 保存配置
     */
    @FXML
    public void saveGeneratorConfig() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("保存当前配置");
        dialog.setContentText("请输入配置名称");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String name = result.get();
            if (StringUtils.isEmpty(name)) {
                AlertUtil.showErrorAlert("名称不能为空");
                return;
            }
            _LOG.info("user choose name: {}", name);
            try {
                GeneratorConfig generatorConfig = getGeneratorConfigFromUI();
                generatorConfig.setName(name);
                ConfigHelper.saveGeneratorConfig(generatorConfig);
            } catch (Exception e) {
                AlertUtil.showErrorAlert("删除配置失败");
            }
        }
    }

    /**
     * 打开生成文件夹 - 代码产生目录
     */
    @FXML
    public void openTargetFolder() {
        GeneratorConfig generatorConfig = getGeneratorConfigFromUI();
        String projectFolder = generatorConfig.getProjectFolder();
        try {
            Desktop.getDesktop().browse(new File(projectFolder).toURI());
        }catch (Exception e) {
            AlertUtil.showErrorAlert("打开目录失败，请检查目录是否填写正确" + e.getMessage());
        }
    }

    /**
     * 代码生成 - 传入参数检查
     * @return
     */
    private String validateConfig() {
        String projectFolder = txtProjectFolder.getText();
        if (StringUtils.isEmpty(projectFolder))  {
            return "项目目录不能为空";
        }else if(StringUtils.isEmpty(txtModuleName.getText()))  {
            return "模块名不能为空";
        }else if(StringUtils.isEmpty(txtAuthor.getText()))  {
            return "作者不能为空";
        }else if(StringUtils.isEmpty(txtProjectPackage.getText()))  {
            return "项目包名不能为空";
        }else if(StringUtils.isEmpty(txtTargetProject.getText()))  {
            return "存放目录不能为空";
        }

        return null;
    }

    /**
     * 检查并创建不存在的文件夹
     *
     * @return
     */
    private boolean checkDirs(GeneratorConfig config) {
        List<String> dirs = new ArrayList<>();
        dirs.add(config.getProjectFolder());
        dirs.add(FilenameUtils.normalize(config.getProjectFolder().concat("/").concat(config.getModelPackageTargetFolder())));
        boolean haveNotExistFolder = false;
        for (String dir : dirs) {
            File file = new File(dir);
            if (!file.exists()) {
                haveNotExistFolder = true;
            }
        }
        if (haveNotExistFolder) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText(FOLDER_NO_EXIST);
            Optional<ButtonType> optional = alert.showAndWait();
            if (optional.isPresent()) {
                if (ButtonType.OK == optional.get()) {
                    try {
                        for (String dir : dirs) {
                            FileUtils.forceMkdir(new File(dir));
                        }
                        return true;
                    } catch (Exception e) {
                        AlertUtil.showErrorAlert("创建目录失败，请检查目录是否是文件而非目录");
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 左侧数据库列表初始装载
     */
    private void leftDBInitial(){
        tvLeftDB.setShowRoot(false);
        tvLeftDB.setRoot(new TreeItem<>());
        Callback<TreeView<String>, TreeCell<String>> defaultCellFactory = TextFieldTreeCell.forTreeView();

        tvLeftDB.setCellFactory((TreeView<String> tv) -> {
            TreeCell<String> cell = defaultCellFactory.call(tv);
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                int level = tvLeftDB.getTreeItemLevel(cell.getTreeItem());
                TreeCell<String> treeCell = (TreeCell<String>) event.getSource();
                TreeItem<String> treeItem = treeCell.getTreeItem();
                if (level == 1) {
                    final ContextMenu contextMenu = new ContextMenu();
                    MenuItem item1 = new MenuItem("关闭连接");
                    item1.setOnAction(event1 -> treeItem.getChildren().clear());
                    MenuItem item2 = new MenuItem("编辑连接");
                    item2.setOnAction(event1 -> {
                        DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
                        DbConnectionController controller = (DbConnectionController) loadFXMLPage("编辑数据库连接", FXMLPage.NEW_CONNECTION, false);
                        controller.setMainUIController(this);
                        controller.setConfig(selectedConfig);
                        controller.showDialogStage();
                    });
                    MenuItem item3 = new MenuItem("删除连接");
                    item3.setOnAction(event1 -> {
                        DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
                        try {
                            ConfigHelper.deleteDatabaseConfig(selectedConfig);
                            this.loadLeftDBTree();
                        } catch (Exception e) {
                            AlertUtil.showErrorAlert("Delete connection failed! Reason: " + e.getMessage());
                        }
                    });
                    contextMenu.getItems().addAll(item1, item2, item3);
                    cell.setContextMenu(contextMenu);
                }

                //鼠标点击 + Ctrl
                if(event.getClickCount() == 1 && event.isControlDown()){
                    if(treeItem == null) {
                        return ;
                    }

                    if (level == 2) { // left DB tree level3
                        selectedDatabaseConfig = (DatabaseConfig) treeItem.getParent().getGraphic().getUserData();

                        if(SELECTED_BACKGROUND.equals(cell.getStyle())){
                            //反选，清除背景色
                            cell.setStyle("");
                            listViewTables.getItems().remove(treeCell.getTreeItem().getValue());
                        }else {
                            //选中，设置背景色
                            cell.setStyle(SELECTED_BACKGROUND);
                            listViewTables.getItems().add(treeCell.getTreeItem().getValue());
                        }
                    }
                }

                //鼠标双击
                if (event.getClickCount() == 2) {
                    if(treeItem == null) {
                        return ;
                    }
                    treeItem.setExpanded(true);
                    if (level == 1) {
                        DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
                        try {
                            List<String> tables = DbUtil.getTableNames(selectedConfig);
                            if (tables != null && tables.size() > 0) {
                                ObservableList<TreeItem<String>> children = cell.getTreeItem().getChildren();
                                children.clear();
                                for (String tableName : tables) {
                                    TreeItem<String> newTreeItem = new TreeItem<>();
                                    ImageView imageView = new ImageView("icons/table.png");
                                    imageView.setFitHeight(16);
                                    imageView.setFitWidth(16);
                                    newTreeItem.setGraphic(imageView);
                                    newTreeItem.setValue(tableName);
                                    children.add(newTreeItem);
                                }
                            }
                        } catch (SQLRecoverableException e) {
                            _LOG.error(e.getMessage(), e);
                            AlertUtil.showErrorAlert("连接超时");
                        } catch (Exception e) {
                            _LOG.error(e.getMessage(), e);
                            AlertUtil.showErrorAlert(e.getMessage());
                        }
                    } else if (level == 2) { // left DB tree level3
                        String tableName = treeCell.getTreeItem().getValue();
                        selectedDatabaseConfig = (DatabaseConfig) treeItem.getParent().getGraphic().getUserData();
                        this.tableName = tableName;

                        // 清除其它已选的表
                        for(Node node1 : cell.getParent().getChildrenUnmodifiable()){
                            node1.setStyle("");
                        }
                        List<String> lstTables = new ArrayList<>();
                        lstTables.add(tableName);
                        ObservableList<String> observableList = FXCollections.observableList(lstTables);
                        listViewTables.setItems(observableList);

                        treeCell.setStyle(SELECTED_BACKGROUND);
                    }
                }
            });

            return cell;
        });

        loadLeftDBTree();
    }

    void loadLeftDBTree() {
        TreeItem rootTreeItem = tvLeftDB.getRoot();
        rootTreeItem.getChildren().clear();
        try {
            List<DatabaseConfig> dbConfigs = ConfigHelper.loadDatabaseConfig();
            for (DatabaseConfig dbConfig : dbConfigs) {
                TreeItem<String> treeItem = new TreeItem<>();
                treeItem.setValue(dbConfig.getName());
                ImageView dbImage = new ImageView("icons/computer.png");
                dbImage.setFitHeight(16);
                dbImage.setFitWidth(16);
                dbImage.setUserData(dbConfig);
                treeItem.setGraphic(dbImage);
                rootTreeItem.getChildren().add(treeItem);
            }
        } catch (Exception e) {
            _LOG.error("connect db failed, reason: {}", e);
            AlertUtil.showErrorAlert(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    private void setTooltip() {
        choiceBoxEncoding.setTooltip(new Tooltip("生成文件的编码，必选"));
        cbOverrideEntity.setTooltip(new Tooltip("重新生成时把原实体类文件覆盖"));
    }

    public GeneratorConfig getGeneratorConfigFromUI() {
        GeneratorConfig generatorConfig = new GeneratorConfig();
        generatorConfig.setProjectFolder(txtProjectFolder.getText());
        //src/main/java
        generatorConfig.setModelPackageTargetFolder(txtTargetProject.getText());
        generatorConfig.setAuthor(txtAuthor.getText());
        generatorConfig.setSuperController(txtSuperController.getText());
        generatorConfig.setProjectPackage(txtProjectPackage.getText());
        generatorConfig.setModuleName(txtModuleName.getText());

        generatorConfig.setEncoding(choiceBoxEncoding.getValue());
        generatorConfig.setSupportLombok(cbSupportLombok.isSelected());
        generatorConfig.setOverrideEntity(cbOverrideEntity.isSelected());
        generatorConfig.setGenController(cbGenController.isSelected());

        return generatorConfig;
    }

    public void setGeneratorConfigIntoUI(GeneratorConfig generatorConfig) {
        txtProjectFolder.setText(generatorConfig.getProjectFolder());
        txtTargetProject.setText(generatorConfig.getModelPackageTargetFolder());
        txtSuperController.setText(generatorConfig.getSuperController());
        txtAuthor.setText(generatorConfig.getAuthor());
        txtModuleName.setText(generatorConfig.getModuleName());
        cbOverrideEntity.setSelected(generatorConfig.isOverrideEntity());
        cbSupportLombok.setSelected(generatorConfig.isSupportLombok());
        txtProjectPackage.setText(generatorConfig.getProjectPackage());
        choiceBoxEncoding.setValue(generatorConfig.getEncoding());
    }
}

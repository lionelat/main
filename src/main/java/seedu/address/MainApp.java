package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import seedu.address.commons.core.Config;
import seedu.address.commons.core.EventsCenter;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.Version;
import seedu.address.commons.events.model.WishBookChangedEvent;
import seedu.address.commons.events.ui.ExitAppRequestEvent;
import seedu.address.commons.exceptions.DataConversionException;
import seedu.address.commons.util.ConfigUtil;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyWishBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.WishBook;
import seedu.address.model.WishTransaction;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.storage.UserPrefsStorage;
import seedu.address.storage.WishBookStorage;
import seedu.address.storage.WishTransactionStorage;
import seedu.address.storage.XmlWishBookStorage;
import seedu.address.storage.XmlWishTransactionStorage;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * The main entry point to the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(1, 3, 1, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected WishTransaction wishTransaction;
    protected Config config;
    protected UserPrefs userPrefs;


    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing WishBook ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        userPrefs = initPrefs(userPrefsStorage);
        WishBookStorage wishBookStorage = new XmlWishBookStorage(userPrefs.getWishBookFilePath());
        WishTransactionStorage wishTransactionStorage =
                new XmlWishTransactionStorage(userPrefs.getWishTransactionFilePath());
        storage = new StorageManager(wishBookStorage, wishTransactionStorage, userPrefsStorage);

        initLogging(config);

        wishTransaction = initWishTransaction(storage, userPrefs);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model);

        ui = new UiManager(logic, config, userPrefs);

        initEventsCenter();
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s wish book and {@code userPrefs}. <br>
     * The data from the sample wish book will be used instead if {@code storage}'s wish book is not found,
     * or an empty wish book will be used instead if errors occur when reading {@code storage}'s wish book.
     */
    private Model initModelManager(Storage storage, UserPrefs userPrefs) {
        Optional<ReadOnlyWishBook> wishBookOptional;
        ReadOnlyWishBook initialData;
        try {
            wishBookOptional = storage.readWishBook();
            if (!wishBookOptional.isPresent()) {
                logger.info("Data file not found. Will be starting with a sample WishBook");
            }
            initialData = wishBookOptional.orElseGet(SampleDataUtil::getSampleWishBook);
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct FORMAT. Will be starting with an empty WishBook");
            initialData = new WishBook();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty WishBook");
            initialData = new WishBook();
        }

        seedWishTransaction(initialData);
        return new ModelManager(initialData, wishTransaction, userPrefs);
    }

    /**
     * Seeds {@code wishTransaction} with data from wishbook if {@code wishTransaction} is empty.
     */
    private void seedWishTransaction(ReadOnlyWishBook initialData) {
        wishTransaction = wishTransaction.isEmpty() ? new WishTransaction(initialData) : wishTransaction;
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s wish book and {@code userPrefs}. <br>
     * The data from the sample wish book will be used instead if {@code storage}'s wish book is not found,
     * or an empty wish book will be used instead if errors occur when reading {@code storage}'s wish book.
     */
    private WishTransaction initWishTransaction(Storage storage, UserPrefs userPrefs) {
        Optional<WishTransaction> wishTransactionOptional;
        WishTransaction initialData;
        try {
            wishTransactionOptional = storage.readWishTransaction();
            if (!wishTransactionOptional.isPresent()) {
                logger.info("Data file not found. Will be starting with a sample WishTransaction file");
            }
            initialData = wishTransactionOptional.orElseGet(SampleDataUtil::getSampleWishTransaction);
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct FORMAT. Will be starting with an empty WishBook");
            initialData = new WishTransaction();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty WishBook");
            initialData = new WishTransaction();
        }

        return initialData;
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br>
     * The default file path {@code Config#DEFAULT_CONFIG_FILE} will be used instead
     * if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct FORMAT. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct FORMAT. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty WishBook");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    private void initEventsCenter() {
        EventsCenter.getInstance().registerHandler(this);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting WishBook " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping Wish Book ] =============================");
        ui.stop();
        try {
            storage.saveBackup();
            storage.saveWishTransaction(wishTransaction);
            storage.saveUserPrefs(userPrefs);
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        } catch (DataConversionException e) {
            logger.severe("Failed to convert file contents to correct FORMAT " + StringUtil.getDetails(e));
        }
        Platform.exit();
        System.exit(0);
    }

    @Subscribe
    public void handleExitAppRequestEvent(ExitAppRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        stop();
    }

    @Subscribe
    public void handleWishBookChangedEvent(WishBookChangedEvent wishBookChangedEvent) {
        wishTransaction = wishBookChangedEvent.wishTransaction;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

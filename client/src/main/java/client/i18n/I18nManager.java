package client.i18n;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Text;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nManager {
    private static I18nManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private NumberFormat numberFormat;
    private DateTimeFormatter dateTimeFormatter;

    private I18nManager() {
        setLocale(new Locale("ru"));
    }

    public static I18nManager getInstance() {
        if (instance == null) instance = new I18nManager();
        return instance;
    }

    public void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("messages.messages", locale);
        numberFormat = NumberFormat.getInstance(locale);
        dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale);
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void updateAllTexts(Node root) {
        if (root instanceof Labeled) {
            Object key = root.getUserData();
            if (key instanceof String) {
                ((Labeled) root).setText(bundle.getString((String) key));
            }
        } else if (root instanceof TextInputControl) {
            Object key = root.getUserData();
            if (key instanceof String) {
                ((TextInputControl) root).setPromptText(bundle.getString((String) key));
            }
        } else if (root instanceof Text) {
            Object key = root.getUserData();
            if (key instanceof String) {
                ((Text) root).setText(bundle.getString((String) key));
            }
        }

        if (root instanceof Parent) {
            for (Node child : ((Parent) root).getChildrenUnmodifiable()) {
                updateAllTexts(child);
            }
        }
    }
}
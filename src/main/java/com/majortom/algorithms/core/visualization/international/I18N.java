package com.majortom.algorithms.core.visualization.international;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18N {
    // 默认设置为中文
    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.CHINESE);

    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
    }

    /**
     * 获取当前语言包
     */
    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("language.language", locale.get());
    }

    /**
     * 创建一个动态绑定的字符串对象
     * 
     * @param key 资源文件中的键
     * @return 随语言变化自动更新的 StringBinding
     */
    public static StringBinding createStringBinding(String key) {
        return Bindings.createStringBinding(
                () -> getBundle().getString(key),
                locale);
    }
}
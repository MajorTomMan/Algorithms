package com.majortom.algorithms.visualization.international;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化工具类。
 *
 * <p>控制器和模块定义都通过这里读取文案。UI 控件通常绑定
 * {@link #createStringBinding(String)}，当 {@link #setLocale(Locale)} 切换语言时，
 * 绑定文本会自动刷新。</p>
 */
public class I18N {
    /**
     * 当前语言环境属性。
     */
    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.CHINESE);

    /**
     * 获取语言环境属性，供 JavaFX binding 监听。
     *
     * @return 当前语言环境属性
     */
    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    /**
     * 获取当前语言环境。
     *
     * @return 当前 Locale
     */
    public static Locale getLocale() {
        return locale.get();
    }

    /**
     * 设置当前语言环境。
     *
     * @param newLocale 新语言环境
     */
    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
    }

    /**
     * 获取当前语言包。
     *
     * @return 当前语言对应的 ResourceBundle
     */
    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("language.language", locale.get());
    }

    /**
     * 读取并格式化国际化文本。
     *
     * @param key 资源 key
     * @param args 格式化参数
     * @return 国际化文本；缺失时返回 key 本身
     */
    public static String text(String key, Object... args) {
        try {
            String pattern = getBundle().getString(key);
            return (args == null || args.length == 0) ? pattern : String.format(pattern, args);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * 创建一个动态绑定的字符串对象。
     * 
     * @param key 资源文件中的键
     * @return 随语言变化自动更新的 StringBinding
     */
    public static StringBinding createStringBinding(String key) {
        return Bindings.createStringBinding(
                () -> text(key),
                locale);
    }
}

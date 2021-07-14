package com.tencent.wmpf.pos.base;

import com.tencent.wmpf.pos.utils.LogUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gaochujia on 2020-12-25.
 */

public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 将实体类转换成请求参数,以map<k,v>形式返回
     *
     * @return
     */
    public Map<String, String> toMap() {
        Class<? extends BaseEntity> clazz = this.getClass();

        Field[] fields = clazz.getDeclaredFields();

        if (fields == null || fields.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        try {
            for (Field field : fields) {
                field.setAccessible(true);
                params.put(field.getName(), String.valueOf(field.get(this)));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        LogUtil.log("", "map 长度 >> " + params.size());
        LogUtil.log("", "map 内容 >> " + params.toString());

        return params;
    }
}

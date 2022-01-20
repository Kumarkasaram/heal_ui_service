package com.heal.dashboard.service.dao.mysql;

import com.datastax.driver.core.Session;
import com.heal.dashboard.service.util.CommonUtils;

public abstract class CommonCassandraDao {
    public void checkConnection(Session session) {
        CommonUtils.checkConnection(session);
    }
}

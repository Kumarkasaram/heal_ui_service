package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import com.heal.dashboard.service.beans.MasterFeaturesBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.FeaturesDao;

@RunWith(SpringRunner.class)
public class MasterFeaturesBLTest {


    @InjectMocks
   private MasterFeaturesBL masterFeaturesBL;

    @Mock
    FeaturesDao jdbcTemplateDao;
    List<MasterFeaturesBean> masterFeaturesBeans;

    @Before
    public void setup() {
        masterFeaturesBeans= new ArrayList<>();
        MasterFeaturesBean masterFeaturesBean = new MasterFeaturesBean();
        masterFeaturesBean.setId(1);
        masterFeaturesBean.setName("UploadPage");
        masterFeaturesBean.setEnabled(true);
        masterFeaturesBeans.add(masterFeaturesBean);

    }

    @Test
    public void getClientValidation_Success() throws Exception {
        Assert.assertEquals("7640123a-fbde-4fe5-9812-581cd1e3a9c1", masterFeaturesBL.clientValidation(null,"7640123a-fbde-4fe5-9812-581cd1e3a9c1").getAuthToken());
    }

    @Test
    public void processData() throws Exception {
        Mockito.when(jdbcTemplateDao.getMasterFeatures()).thenReturn(masterFeaturesBeans);
        Assert.assertEquals(masterFeaturesBeans.get(0).getName(), masterFeaturesBL.process(masterFeaturesBeans).get(0).getName());
    }
}

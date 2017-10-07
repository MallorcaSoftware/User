package com.mallorcasoftware.user;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class BaseTest {
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
}

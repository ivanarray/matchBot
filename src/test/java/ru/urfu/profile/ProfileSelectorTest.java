package ru.urfu.profile;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class ProfileSelectorTest extends TestCase {

    /**
     * Тест, проверяющий корректность работы выбора следующего профиля
     * @throws Exception может бросить ошибку, если такой id уже есть
     */
    public void testGetNextProfile() throws Exception {
        var profiles = new ProfileData();
        var profile0 = new Profile(2, profiles);
        var profile1 = new Profile(3, profiles);
        profiles.addProfile(profile0);
        profiles.addProfile(profile1);
        var ps = new ProfileSelector(profiles, profile0);
        var next = ps.getNextProfile();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(next.equals(profile0) || next.equals(profile1));
            next = ps.getNextProfile();
        }
    }
}
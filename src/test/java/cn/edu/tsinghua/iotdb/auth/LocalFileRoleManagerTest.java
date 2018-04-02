package cn.edu.tsinghua.iotdb.auth;

import cn.edu.tsinghua.iotdb.auth.Role.LocalFileRoleManager;
import cn.edu.tsinghua.iotdb.auth.entity.PathPrivilege;
import cn.edu.tsinghua.iotdb.auth.entity.Role;
import cn.edu.tsinghua.iotdb.utils.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocalFileRoleManagerTest {
    private File testFolder;
    private LocalFileRoleManager manager;

    @Before
    public void setUp() throws Exception {
        EnvironmentUtils.envSetUp();
        testFolder = new File("test/");
        testFolder.mkdirs();
        manager = new LocalFileRoleManager(testFolder.getPath());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testFolder);
        EnvironmentUtils.cleanEnv();
    }

    @Test
    public void test() throws AuthException {
        Role[] roles = new Role[5];
        for (int i = 0; i < roles.length; i++) {
            roles[i] = new Role("role" + i);
            for(int j = 0; j <= i; j++) {
                PathPrivilege pathPrivilege = new PathPrivilege("root.a.b.c" + j);
                pathPrivilege.privileges.add(j);
                roles[i].privilegeList.add(pathPrivilege);
            }
        }

        // create
        Role role = manager.getRole(roles[0].name);
        assertEquals(null, role);
        for (Role role1 : roles) assertEquals(true, manager.createRole(role1.name));
        for (Role role1 : roles) {
            role = manager.getRole(role1.name);
            assertEquals(role1.name, role.name);
        }

        assertEquals(false, manager.createRole(roles[0].name));
        boolean caught = false;
        try {
            manager.createRole("too");
        } catch (AuthException e) {
            caught = true;
        }
        assertEquals(true, caught);

        // delete
        assertEquals(false, manager.deleteRole("not a role"));
        assertEquals(true, manager.deleteRole(roles[roles.length-1].name));
        assertEquals(null, manager.getRole(roles[roles.length-1].name));
        assertEquals(false, manager.deleteRole(roles[roles.length-1].name));

        // grant privilege
        role = manager.getRole(roles[0].name);
        String path = "root.a.b.c";
        int privilegeId = 0;
        assertEquals(false, role.hasPrivilege(path, privilegeId));
        assertEquals(true, manager.grantPrivilegeToRole(role.name, path, privilegeId));
        assertEquals(true, manager.grantPrivilegeToRole(role.name, path, privilegeId + 1));
        assertEquals(false, manager.grantPrivilegeToRole(role.name, path, privilegeId));
        role = manager.getRole(roles[0].name);
        assertEquals(true, role.hasPrivilege(path, privilegeId));
        caught = false;
        try {
            manager.grantPrivilegeToRole("not a role", path, privilegeId);
        } catch (AuthException e) {
            caught = true;
        }
        assertEquals(true, caught);
        caught = false;
        try {
            manager.grantPrivilegeToRole(role.name, path, -1);
        } catch (AuthException e) {
            caught = true;
        }
        assertEquals(true, caught);

        // revoke privilege
        role = manager.getRole(roles[0].name);
        assertEquals(true, manager.revokePrivilegeFromRole(role.name, path, privilegeId));
        assertEquals(false, manager.revokePrivilegeFromRole(role.name, path, privilegeId));
        caught = false;
        try {
            manager.revokePrivilegeFromRole("not a role", path, privilegeId);
        } catch (AuthException e) {
            caught = true;
        }
        assertEquals(true, caught);
        caught = false;
        try {
            manager.revokePrivilegeFromRole(role.name, path, -1);
        } catch (AuthException e) {
            caught = true;
        }
        assertEquals(true, caught);

        // list roles
        List<String> rolenames = manager.listAllRoles();
        rolenames.sort(null);
        for(int i = 0; i < roles.length - 1; i++) {
            assertEquals(roles[i].name, rolenames.get(i));
        }
    }
}

package javaguide.tests;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ModelTest {
  
  //#test-model
  public class User {
    private Integer id;
    private String name;
    
    public User(final Integer id, final String name) {
      this.id = id;
      this.name = name;
    }
  }
  
  public class Role {
    private String name;
    
    public Role(final String name) {
      this.name = name;
    }
  }
  //#test-model
  
  //#test-model-repository
  public interface UserRepository {
    public Set<Role> findUserRoles(User user);
  }
  
  public class UserRepositoryEbean implements UserRepository {
    @Override
    public Set<Role> findUserRoles(User user) {
      // Get roles from DB
      //###replace:      ...
      return null;
    }
  }
  //#test-model-repository
  
  //#test-model-service
  public class UserService {
    private final UserRepository userRepository;
    
    public UserService(final UserRepository userRepository) {
      this.userRepository = userRepository;
    }
    
    public boolean isAdmin(final User user) {
      final Set<Role> roles = userRepository.findUserRoles(user);
      for (Role role: roles) {
        if (role.name.equals("ADMIN"))
          return true;
      }
      return false;
    }
  }
  //#test-model-service
  
  //#test-model-test
  @Test
  public void testIsAdmin() {
    
    // Create and train mock repository
    UserRepository repositoryMock = mock(UserRepository.class);
    Set<Role> roles = new HashSet<Role>();
    roles.add(new Role("ADMIN"));
    when(repositoryMock.findUserRoles(any(User.class))).thenReturn(roles);
    
    // Test Service
    UserService userService = new UserService(repositoryMock);
    User user = new User(1, "Johnny Utah");
    assertTrue(userService.isAdmin(user));
    verify(repositoryMock).findUserRoles(user);
  }
  //#test-model-test
}


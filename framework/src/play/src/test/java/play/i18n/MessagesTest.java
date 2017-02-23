/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.i18n;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MessagesTest {
    
    @Test
    public void wrapNoVarArgsToEmptyList(){
        final List<Object> resultList = Messages.wrapArgsToListIfNeeded();
        assertThat(resultList).isNotNull();
        assertThat(resultList.size()).isEqualTo(0);
    }
    
    @Test
    public void wrapOneStringElementToList(){
        final List<String> resultList = Messages.wrapArgsToListIfNeeded("Croissant");
        assertThat(resultList).isNotNull();
        assertThat(resultList.size()).isEqualTo(1);
        assertThat(resultList.get(0)).isEqualTo("Croissant");
    }
    
    @Test
    public void wrapTwoStringElementsToList(){
        final List<String> resultList = Messages.wrapArgsToListIfNeeded("Croissant", "Baguette");
        assertThat(resultList).isNotNull();
        assertThat(resultList.size()).isEqualTo(2);
        assertThat(resultList.contains("Croissant")).isTrue();
        assertThat(resultList.contains("Baguette")).isTrue();
    }
    
    @Test
    public void wrapOneListElementReturnsIt(){
        final List<String> stringList = Arrays.asList("Croissant", "Baguette");
        final List<List<String>> resultList = Messages.wrapArgsToListIfNeeded(stringList);
        assertThat(resultList).isNotNull();
        assertThat(resultList.size()).isEqualTo(2);
        assertThat(resultList.contains("Croissant")).isTrue();
        assertThat(resultList.contains("Baguette")).isTrue();
    }
    
    @Test
    public void wrapOneListAndOneStringShouldNotFlattenTheList(){
        final List<String> stringList = Arrays.asList("Croissant", "Baguette");
        final List<Object> resultList = Messages.wrapArgsToListIfNeeded(stringList, "Pain");
        assertThat(resultList).isNotNull();
        assertThat(resultList.size()).isEqualTo(2);
        assertThat(resultList.contains(stringList)).isTrue();
        assertThat(resultList.contains("Pain")).isTrue();
        
    }
}

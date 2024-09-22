/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.durabilityviewer.fabrictools;

import java.util.List;

/**
 *
 * @author gbl
 */
public interface IConfiguration {
    
    List<String> getKeys();
    Object getValue(String option);
    boolean setValue(String option, Object value);
    
    Object getDefault(String option);
    Object getMin(String option);
    Object getMax(String option);
    String getTooltip(String option);

    boolean isSelectList(String option);
    String[] getListOptions(String option);
}

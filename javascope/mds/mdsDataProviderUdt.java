/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mds;

/**
 * @author manduchi
 */
public class mdsDataProviderUdt extends mdsDataProvider{
    @Override
    protected mdsConnection getConnection() {
        return new mdsConnectionUdt();
    }
}

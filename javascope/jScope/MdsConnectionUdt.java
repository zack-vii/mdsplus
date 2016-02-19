/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jScope;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author manduchi
 */
public class MdsConnectionUdt extends MdsConnection{
    @Override
    public void connectToServer() throws IOException {
        if(provider != null){
            host = getProviderHost();
            port = getProviderPort();
            user = getProviderUser();
            MdsIpProtocolWrapper mipw = new MdsIpProtocolWrapper("udt://" + host + ":" + port);
            dis = mipw.getInputStream();
            dos = new DataOutputStream(mipw.getOutputStream());
        }
    }
}

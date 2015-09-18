function [ status ] = mdsclose()
%mdsconnect - connect to a remote mdsplus data server.
%
%     This routine will close the tree at the top of the open tree stack.
%
%      Previous incarnations also disconnected thin-client (mdsconnect) connections.
%      THAT IS NO LONGER DONE
%
import MDSplus.Data
global MDSplus_Connection_Obj
if isjava(MDSplus_Connection_Obj)
    try
        MDSplus_Connection_Obj.get('TreeClose()');
        status = true;
    catch err
        if nargout==1
            status = false;
        else
            rethrow(err)
        end
    end
else
    value = mdsvalue('TreeClose()');
    status = logical(bitand(uint32(value),1));
    if ~status
        error(mdsgetmsg(value))
    end
end


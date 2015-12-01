#include <stdio.h>
#include <ncidef.h>
#include <treeshr_hooks.h>
extern char *TreeGetPath(int);
int Notify(TreeshrHookType htype, TREE_INFO * info, int nid)
//int Notify(TreeshrHookType htype, char *tree, int shot, int nid)
{
  char *name;
  char def_name[50];
  char *na = "N/A";
  char *path = na;
  sprintf(def_name, "Hook number %d", htype);
  name = def_name;
  switch (htype) {
  case OpenTree:
    return 1
//    name = "OpenTree";
//    break;
  case OpenTreeEdit:
    return 1
//    name = "OpenTreeEdit";
//    break;
  case RetrieveTree:
    return 0;
  case WriteTree:
    return 1
//    name = "WriteTree";
//    break;
  case CloseTree:
    return 1
//    name = "CloseTree";
//    break;
  case OpenNCIFileWrite:
    return 1;
  case OpenDataFileWrite:
    return 1;
  case GetData:
    return 1;
    //name = "GetData";
    //path = TreeGetPath(nid);
    //break;
  case GetNci:
    return 1;
    //name = "GetNci";
    //path = TreeGetPath(nid);
    //break;
  case PutData:
    name = "PutData";
    path = TreeGetPath(nid);
   	NCI *nci = info_ptr->data_file->asy_nci->nci;
    if(nci->flags & NciM_EVENT_ON_PUT)
    {
        TAG_INFO *tag_info = info_ptr->tag_info;
        if(*tag_info==null)
            return 1
        char *tag = taginfo->name;
        MDSEvent(tag, 0, (char *)0);
        printf("%s hook called for tree=%s, shot=%d, tag=%s\n", name, info->treenam, info->shot, tag);
    }
    return 1;
  case PutNci:
    return 1;
    //name = "PutNci";
    //path = TreeGetPath(nid);
    //break;
  }
  printf("%s hook called for tree=%s, shot=%d, node=%s\n", name, info->treenam, info->shot, path);
  if (path != na && path != (char *)0)
    free(path);
  return 1;
}

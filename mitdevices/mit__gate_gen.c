/*
Copyright (c) 2017, Massachusetts Institute of Technology All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
#include "mitdevices_msg.h"
#include "mds_gendevice.h"
#include "mit__gate_gen.h"
EXPORT int mit__gate__add(struct descriptor *name_d_ptr, struct descriptor *dummy_d_ptr __attribute__ ((unused)), int *nid_ptr)
{
  static DESCRIPTOR(library_d, "MIT$DEVICES");
  static DESCRIPTOR(model_d, "MIT__GATE");
  static DESCRIPTOR_CONGLOM(conglom_d, &library_d, &model_d, 0, 0);
  int usage = TreeUSAGE_DEVICE;
  int curr_nid, old_nid, head_nid, status;
  long int flags = NciM_WRITE_ONCE;
  NCI_ITM flag_itm[] = { {2, NciSET_FLAGS, 0, 0}, {0, 0, 0, 0} };
  char *name_ptr = strncpy(malloc(name_d_ptr->length + 1), name_d_ptr->pointer, name_d_ptr->length);
  flag_itm[0].pointer = (unsigned char *)&flags;
  name_ptr[name_d_ptr->length] = 0;
  status = TreeStartConglomerate(MIT__GATE_K_CONG_NODES);
  if (STATUS_NOT_OK)
    return status;
  status = TreeAddNode(name_ptr, &head_nid, usage);
  if (STATUS_NOT_OK)
    return status;
  *nid_ptr = head_nid;
  status = TreeSetNci(head_nid, flag_itm);
  status = TreePutRecord(head_nid, (struct descriptor *)&conglom_d, 0);
  if (STATUS_NOT_OK)
    return status;
  status = TreeGetDefaultNid(&old_nid);
  if (STATUS_NOT_OK)
    return status;
  status = TreeSetDefaultNid(head_nid);
  if (STATUS_NOT_OK)
    return status;
 ADD_NODE(:COMMENT, TreeUSAGE_TEXT)
 ADD_NODE_INTEGER(:TRIGGER_MODE, 0, TreeUSAGE_NUMERIC)
      flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
 ADD_NODE(:START_LOW, TreeUSAGE_NUMERIC)
      flags |= NciM_NO_WRITE_MODEL;
  flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
 ADD_NODE(:TRIGGER, TreeUSAGE_NUMERIC)
      flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
 ADD_NODE(:EVENT, TreeUSAGE_TEXT)
      flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
 ADD_NODE(:DURATION, TreeUSAGE_NUMERIC)
      flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
 ADD_NODE(:PULSE_TIME, TreeUSAGE_NUMERIC)
      flags |= NciM_NO_WRITE_SHOT;
  status = TreeSetNci(curr_nid, flag_itm);
#define expr " MIT__GATE(TRIGGER,PULSE_TIME,DURATION) "
 ADD_NODE_EXPR(:GATE_OUT, TreeUSAGE_NUMERIC)
#undef expr
      flags |= NciM_WRITE_ONCE;
  status = TreeSetNci(curr_nid, flag_itm);
#define expr " GATE_OUT[GETNCI(START_LOW,'STATE')] "
 ADD_NODE_EXPR(:EDGES_R, TreeUSAGE_NUMERIC)
#undef expr
      flags |= NciM_WRITE_ONCE;
  status = TreeSetNci(curr_nid, flag_itm);
#define expr " GATE_OUT[!GETNCI(START_LOW,'STATE')] "
 ADD_NODE_EXPR(:EDGES_F, TreeUSAGE_NUMERIC)
#undef expr
      flags |= NciM_WRITE_ONCE;
  status = TreeSetNci(curr_nid, flag_itm);
  status = TreeEndConglomerate();
  if (STATUS_NOT_OK)
    return status;
  return (TreeSetDefaultNid(old_nid));
}

EXPORT int mit__gate__part_name(struct descriptor *nid_d_ptr __attribute__ ((unused)), struct descriptor *method_d_ptr __attribute__ ((unused)),
			 struct descriptor_d *out_d)
{
  int element = 0, status;
  NCI_ITM nci_list[] = { {4, NciCONGLOMERATE_ELT, 0, 0}, {0, 0, 0, 0} };
  nci_list[0].pointer = (unsigned char *)&element;
  status = TreeGetNci(*(int *)nid_d_ptr->pointer, nci_list);
  if (STATUS_NOT_OK)
    return status;
  switch (element) {
  case (MIT__GATE_N_HEAD + 1):
    StrFree1Dx(out_d);
    break;
  case (MIT__GATE_N_COMMENT + 1):
 COPY_PART_NAME(:COMMENT) break;
  case (MIT__GATE_N_TRIGGER_MODE + 1):
 COPY_PART_NAME(:TRIGGER_MODE) break;
  case (MIT__GATE_N_START_LOW + 1):
 COPY_PART_NAME(:START_LOW) break;
  case (MIT__GATE_N_TRIGGER + 1):
 COPY_PART_NAME(:TRIGGER) break;
  case (MIT__GATE_N_EVENT + 1):
 COPY_PART_NAME(:EVENT) break;
  case (MIT__GATE_N_DURATION + 1):
 COPY_PART_NAME(:DURATION) break;
  case (MIT__GATE_N_PULSE_TIME + 1):
 COPY_PART_NAME(:PULSE_TIME) break;
  case (MIT__GATE_N_GATE_OUT + 1):
 COPY_PART_NAME(:GATE_OUT) break;
  case (MIT__GATE_N_EDGES_R + 1):
 COPY_PART_NAME(:EDGES_R) break;
  case (MIT__GATE_N_EDGES_F + 1):
 COPY_PART_NAME(:EDGES_F) break;
  default:
    status = TreeILLEGAL_ITEM;
  }
  return status;
}

extern int mit__gate___get_setup();
#define free_xd_array { int i; for(i=0; i<0;i++) if(work_xd[i].l_length) MdsFree1Dx(&work_xd[i],0);}
#define error(nid,code,code1) error_code = code1;
EXPORT int mit__gate___get_setup(struct descriptor *nid_d_ptr __attribute__ ((unused)), InGet_setupStruct * in_ptr)
{
  declare_variables(InGet_setupStruct)
    //struct descriptor_xd work_xd[1];
  initialize_variables(InGet_setupStruct)

      read_float(MIT__GATE_N_TRIGGER, trigger);
  read_float(MIT__GATE_N_PULSE_TIME, pulse_time);
  read_float(MIT__GATE_N_DURATION, duration);
  read_integer_error(MIT__GATE_N_TRIGGER_MODE, trigger_mode, TIMING$_INVTRGMOD);
  check_range(trigger_mode, 0, 3, TIMING$_INVTRGMOD);
  build_results_and_return;
}

#undef free_xd_array

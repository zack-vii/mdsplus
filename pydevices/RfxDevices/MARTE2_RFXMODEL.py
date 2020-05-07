from MDSplus import Data
MC = __import__('MARTE2_COMPONENT', globals())

@MC.BUILDER('SimulinkInterfaceGAM', MC.MARTE2_COMPONENT.MODE_GAM)
class MARTE2_RFXMODEL(MC.MARTE2_COMPONENT):
    parameters = [
        {'name' : 'AAGain', 'type' : 'float64'},
        {'name' : 'BV_GAIN', 'type' : 'float64'},
        {'name' : 'CCKp', 'type' : 'float64'},
        {'name' : 'CCTEnd', 'type' : 'float64'},
        {'name' : 'CCType', 'type' : 'float64'},
        {'name' : 'CompMagOn', 'type' : 'float64'},
        {'name' : 'DisruptionDetection', 'type' : 'float64'},
        {'name' : 'EquiFlux', 'type' : 'float64'},
        {'name' : 'EquilNonlinearFactorSaturation', 'type' : 'float64'},
        {'name' : 'FDBK_ON', 'type' : 'float64'},
        {'name' : 'FFWD_ON', 'type' : 'float64'},
        {'name' : 'FS_Bv_RT', 'type' : 'float64'},
        {'name' : 'FS_mmf_RT', 'type' : 'float64'},
        {'name' : 'GainCompRes', 'type' : 'float64'},
        {'name' : 'GainDecoupl', 'type' : 'float64'},
        {'name' : 'HorShiftFFGAIN', 'type' : 'float64'},
        {'name' : 'HorShiftFFON', 'type' : 'float64'},
        {'name' : 'HorShiftQctrlEnable', 'type' : 'float64'},
        {'name' : 'HorShiftRefQctrl', 'type' : 'float64'},
        {'name' : 'HorShiftTestSatFB', 'type' : 'float64'},
        {'name' : 'HorShiftTimeQctrl', 'type' : 'float64'},
        {'name' : 'I_to_B', 'type' : 'float64'},
        {'name' : 'IpCompMagOn', 'type' : 'float64'},
        {'name' : 'K_decoupl', 'type' : 'float64'},
        {'name' : 'Kd', 'type' : 'float64'},
        {'name' : 'Ki', 'type' : 'float64'},
        {'name' : 'Kp', 'type' : 'float64'},
        {'name' : 'LeadLagHorShiftON', 'type' : 'float64'},
        {'name' : 'Lp', 'type' : 'float64'},
        {'name' : 'Max_PVAT_Curr', 'type' : 'float64'},
        {'name' : 'MinIpCurr', 'type' : 'float64'},
        {'name' : 'PVATFilter_den', 'type' : 'float64'},
        {'name' : 'PVATFilter_num', 'type' : 'float64'},
        {'name' : 'RFPCCDeltaIpStar', 'type' : 'float64'},
        {'name' : 'RFPCCDeltaTBumpless', 'type' : 'float64'},
        {'name' : 'RFPCCDeltaTRampDown', 'type' : 'float64'},
        {'name' : 'RFPCCInFiltDen', 'type' : 'float64'},
        {'name' : 'RFPCCInFiltNum', 'type' : 'float64'},
        {'name' : 'RFPCCIpStar', 'type' : 'float64'},
        {'name' : 'RFPCCOutFiltDen', 'type' : 'float64'},
        {'name' : 'RFPCCOutFiltNum', 'type' : 'float64'},
        {'name' : 'RFPCCPOhmMax', 'type' : 'float64'},
        {'name' : 'RFPCCTaup', 'type' : 'float64'},
        {'name' : 'RFPCCTauz', 'type' : 'float64'},
        {'name' : 'ResCablePcat', 'type' : 'float64'},
        {'name' : 'Rmag', 'type' : 'float64'},
        {'name' : 'Rtransf', 'type' : 'float64'},
        {'name' : 'TOKCCTStart', 'type' : 'float64'},
        {'name' : 'TOKCCVLoopThreshold', 'type' : 'float64'},
        {'name' : 'TOKCCVMax', 'type' : 'float64'},
        {'name' : 'TOKCCVRogThreshold', 'type' : 'float64'},
        {'name' : 'TStartEquilIntegralAction', 'type' : 'float64'},
        {'name' : 'TokFastRampUpIpOn', 'type' : 'float64'},
        {'name' : 'TokIpLowQ', 'type' : 'float64'},
        {'name' : 'TokThresholdBpmode', 'type' : 'float64'},
        {'name' : 'TokTstartCheckMode', 'type' : 'float64'},
        {'name' : 'TokTunIpFR', 'type' : 'float64'},
        {'name' : 'TokTunVpcat', 'type' : 'float64'},
        {'name' : 'TokVpcatRampUp', 'type' : 'float64'},
        {'name' : 'Tstep', 'type' : 'float64'},
        {'name' : 'Voltage_Control', 'type' : 'float64'},
        {'name' : 'equilFFDerivativeVLoopDen', 'type' : 'float64'},
        {'name' : 'equilFFDerivativeVLoopNum', 'type' : 'float64'},
        {'name' : 'equilFFDerivativeVRogDen', 'type' : 'float64'},
        {'name' : 'equilFFDerivativeVRogNum', 'type' : 'float64'},
        {'name' : 'equilFFProportionalDen', 'type' : 'float64'},
        {'name' : 'equilFFProportionalNum', 'type' : 'float64'},
        {'name' : 'invAAGain', 'type' : 'float64'},
        {'name' : 'k_comp_res', 'type' : 'float64'},
        {'name' : 'tau_1', 'type' : 'float64'},
        {'name' : 'taupHorShiftFF', 'type' : 'float64'},
        {'name' : 'tauzHorShiftFF', 'type' : 'float64'},
        {'name' : 'time_step', 'type' : 'float64'}]
    inputs = [
        {'name' : 'Horizontal_Shift', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Btw', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Ip', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Field_shaping_forces_OK', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'SoftTerm1', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'SoftTerm2', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Hor_Shift_Shell', 'type' : 'float64', 'dimensions' : 0, 'parameters':{}},
        {'name' : 'Bv', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'I_M', 'type' : 'float64', 'dimensions' : Data.compile('[4]'), 'parameters': {}},
        {'name' : 'I_FS', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'q', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'V_rog', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'V_loop', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'TorFlux', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'time', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Bv_add', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'delta_ip', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'I_FS_add', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'Hor_Shift_Ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'PMAT', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'PCAT', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'TFAT', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'Btw_ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'q_ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'delta_I_FS_add', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'Ip_ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}}]
    outputs = [
        {'name' : 'PCAT_Ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'PMAT_Ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'TFAT_Ref', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'PVAT_Ref', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'FS_I_Ref', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'FS_FF_Ref', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'FS_FB_Ref', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}},
        {'name' : 'time', 'type' : 'float64', 'dimensions' : 0, 'parameters': {}},
        {'name' : 'FF', 'type' : 'float64', 'dimensions' : Data.compile('[8]'), 'parameters': {}}]

    parts = []

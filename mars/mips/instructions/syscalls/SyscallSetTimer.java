package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;

public class SyscallSetTimer extends AbstractSyscall{
    public SyscallSetTimer() {
        super(37, "SetTimerInterrupt");
    }

    public void simulate(ProgramStatement statement) throws ProcessingException {
        int time = RegisterFile.getValue(4);
        int addr = RegisterFile.getValue(5);
        Coprocessor0.updateRegister(11, Coprocessor0.getValue(9) + time);
        try{Memory.getInstance().set(0xffff0010, addr, 4);}catch (Exception ignored){
        }
    }
}

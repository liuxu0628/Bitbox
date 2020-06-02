/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unimelb.bitbox;

import java.util.Objects;

/**
 *
 * @author l2499
 */
public class Operation {

    private final String fileDes;
    private final String event;
    private long position;

    public Operation(String event, String fileDes) {
        this.fileDes = fileDes;
        this.event = event;
        position = -1;
    }

    public Operation(String event, String fileDes, long position) {
        this.fileDes = fileDes;
        this.event = event;
        this.position = position;
    }

    @Override
    public boolean equals(Object obj) {
        //首先判断是否为空
        if (obj != null) {
            //自己和自己比较时,直接返回true
            if (obj == this) {
                return true;
            }
            //判断是否是同类型的对象进行比较
            if (obj instanceof Operation) {
                Operation op = (Operation) obj;
                if (this.event.equals(op.event)
                        && this.fileDes.equals(op.fileDes)
                        && this.position == op.position) {
                    return true;
                }
            }
        }
        return false;
    }
}

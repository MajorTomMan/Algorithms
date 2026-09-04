package com.majortom.algorithms.core.runtime;
import com.majortom.algorithms.core.domain.execution.*;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
/** Small runtime lifecycle owner; domain behavior remains in domain methods. */
public final class ExecutionRuntime {
    private final Clock clock; private final Supplier<String> runIdSupplier;
    public ExecutionRuntime(){this(Clock.systemUTC(),()->UUID.randomUUID().toString());}
    public ExecutionRuntime(Clock clock,Supplier<String> runIdSupplier){this.clock=Objects.requireNonNull(clock);this.runIdSupplier=Objects.requireNonNull(runIdSupplier);}
    public ExecutionResult execute(String operationId,EventSink sink,ExecutionOperation<?> operation){return execute(operationId,operationId,sink,RunControl.unrestricted(),operation);}
    public ExecutionResult execute(String operationId,EventSink sink,RunControl control,ExecutionOperation<?> operation){return execute(operationId,operationId,sink,control,operation);}
    public ExecutionResult execute(String operationId,String source,EventSink sink,RunControl control,ExecutionOperation<?> operation){
        Objects.requireNonNull(operation); RuntimeEventContext c=new RuntimeEventContext(runIdSupplier.get(),operationId,source,sink,control,clock);
        DefaultExecutionControl defaultControl=control instanceof DefaultExecutionControl value?value:null;
        if(defaultControl!=null)defaultControl.bindLifecycle(c::emitLifecycle);
        try{c.emitLifecycle(new RunStartedEvent());c.startCheckpoint();Object output;try(ExecutionEvents.Binding ignored=ExecutionEvents.bind(c)){output=operation.execute();}c.completionCheckpoint();c.emitLifecycle(new RunCompletedEvent());return ExecutionResult.completed(output);}
        catch(ExecutionCancellationException e){return cancelled(c,e.getMessage());}
        catch(InterruptedException e){Thread.currentThread().interrupt();return cancelled(c,"Execution interrupted");}
        catch(EventDeliveryException e){return ExecutionResult.failed(new ExecutionFailure("execution.event.delivery.failed",e.getMessage(),e.getClass().getName()));}
        catch(RuntimeException e){try{c.emitLifecycle(new RunFailedEvent("execution.operation.failed",message(e)));}catch(RuntimeException ignored){}return ExecutionResult.failed(new ExecutionFailure("execution.operation.failed",message(e),e.getClass().getName()));}
        finally{if(defaultControl!=null)defaultControl.unbindLifecycle();}
    }
    private ExecutionResult cancelled(RuntimeEventContext c,String reason){try{c.emitLifecycle(new RunCancelledEvent(reason==null?"Execution cancelled":reason));}catch(RuntimeException ignored){}return ExecutionResult.cancelled();}
    private static String message(Throwable t){String m=t.getMessage();return m==null||m.isBlank()?t.getClass().getSimpleName():m;}
}

/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
the specific language governing rights and limitations under the License.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.datastore;


/**
 * Optional datastore capability for grouping work into an independently
 * cancellable operation.
 * <p>
 * The operation is bound to the calling thread until it is closed. A caller
 * may invoke {@link CancellableOperation#cancel()} from another thread to
 * abort only datastore resources acquired inside this scope. Implementations
 * must prevent the cancelled scope from acquiring new resources. The caller
 * remains responsible for waiting until its worker has actually terminated
 * before reporting a successful stop.
 * </p>
 */
public interface ICancellableDataStore
{
    /**
     * Opens a cancellable operation on the current thread.
     *
     * @return operation handle, which must be closed by the opening thread
     * @throws IllegalStateException if the current thread already owns an
     *         open operation
     */
    CancellableOperation beginCancellableOperation();


    interface CancellableOperation extends AutoCloseable
    {
        /**
         * Requests cancellation and aborts resources currently used by this
         * operation. This method is thread-safe and idempotent.
         */
        void cancel();


        /**
         * @return true after cancellation has been requested
         */
        boolean isCancellationRequested();


        /**
         * Ends the operation scope. Must be invoked by the thread that opened
         * it after all scoped datastore calls have returned.
         */
        @Override
        void close();
    }
}

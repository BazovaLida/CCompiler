.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main PROC
	push ebp
	mov ebp, esp

	mov [esp-0], 0

	pop eax
	mov [esp-8], eax   ;

	mov [esp-0], 5

	pop eax
	mov [esp-4], eax   ;

	mov [esp-0], 90

	push [esp-4]     ;value1_val

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	push [esp-8]     ;b_val

	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

	pop eax
	mov [esp-12], eax   ;

	push [esp-12]     ;j_val

	pop eax
	mov [esp-4], eax   ;

	push [esp-8]     ;b_val

	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret
main ENDP
END start
